package com.pocket.sdk.api.source

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.pocket.sdk.api.endpoint.Credentials
import com.pocket.sdk.api.endpoint.Endpoint
import com.pocket.sdk.api.endpoint.SnowplowEndpoint
import com.pocket.sdk.api.endpoint.unwrapSnowplowApiException
import com.pocket.sdk.api.generated.Modeller
import com.pocket.sdk.api.generated.PocketRemoteStyle
import com.pocket.sdk.api.generated.action.*
import com.pocket.sdk.api.generated.enums.SnowplowAppId
import com.pocket.sdk.api.generated.enums.SnowplowPlatform
import com.pocket.sdk.api.generated.thing.*
import com.pocket.sdk.api.source.SnowplowSource.Companion.JSON_CONFIG
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.network.eclectic.EclecticHttp
import com.pocket.sync.action
import com.pocket.sync.action.Action
import com.pocket.sync.source.FullResultSource
import com.pocket.sync.source.JsonConfig
import com.pocket.sync.source.LimitedSource
import com.pocket.sync.source.result.Status
import com.pocket.sync.source.result.SyncResult
import com.pocket.sync.spec.Syncable
import com.pocket.sync.thing.Thing
import com.pocket.sync.value.StringEnum
import okio.ByteString

private const val TRACKER_VERSION = "pkt-andr"
private const val EVENT_TYPE_UNSTRUCTURED = "ue"

private object Fields {
    const val SCHEMA = "schema"
    const val DATA = "data"
    
    const val PLATFORM = "p" // The platform the app runs on.
    const val TRACKER_VERSION = "tv" // Identifier for Snowplow tracker.
    const val EVENT_TYPE = "e"
    const val APP_ID = "aid" // Unique identifier for website / application.
    const val BR_LANG = "lang" // Language the browser is set to.
    
    const val SEND_TIME = "stm" // Timestamp when event was sent by client device to collector.
    
    const val EVENT_ID = "eid" // Event UUID.
    const val CONTEXTS = "cx" // An array of custom contexts (URL-safe Base64 encoded).
    const val EVENT = "ue_px" // The properties of the event (URL-safe Base64 encoded).
    const val DEVICE_TIME = "dtm" // Timestamp when event occurred, as recorded by client device.
}

private object Schemas {
    const val PAYLOAD_DATA = "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4"
    const val CONTEXTS = "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1"
    const val UNSTRUCT_EVENT = "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0"
}

private const val MAX_ACTIONS_DEFAULT = 25
private const val MAX_ACTIONS_LIMIT = 25

/**
 * Source that supports sending [PocketRemoteStyle.SNOWPLOW] actions to the Snowplow collector.
 * 
 * Snowplow is a new analytics backend. Apps send it "events" (modelled as actions) which can
 * contain "entities" (modelled as things).
 *
 * ## Collector endpoint
 *
 * There is a single collector endpoint that accepts all actions. The collector URL is in the
 * form of `https://<DOMAIN>/com.snowplowanalytics.snowplow/tp2`. For Pocket the domain is
 * `d.getpocket.com`. It accepts both GET and POST requests. But only the latter allows
 * sending actions in batches, which feels like the better choice and this documentation
 * will focus on it.
 *
 * Default batch size in Snowplow 1st party implementation is 10. There is also an option to
 * increase it to 25. It is unclear if the endpoint accepts bigger batches.
 *
 * Send parameters as JSON (`Content-Type: application/json; charset=utf-8`). Both a batch of
 * actions and each individual action needs some wrapping and reformatting to have a specific
 * JSON structure the endpoint expects. Here's how to build it:
 *
 * ## JSON body structure
 *
 * At the top level start with a JSON object comprised of two fields:
 * ```json
 * {
 *   "schema": "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4",
 *   "data": []
 * }
 * ```
 * `data` holds an array of actions. Each action is a JSON object with following fields:
 *  * `p` (platform), choose a value from [SnowplowPlatform] appropriate for your app.
 *  * `tv` (tracker version), define a unique version string with the Data team.
 *  * `e` which defines the type of the event, but it looks like all events are wrapped
 *     in a "unstructured event" and the value of this field is always `ue`.
 *  * `aid` (app ID), choose a value from [SnowplowAppId] corresponding to your app.
 *  * `eid` (event ID) set to `eid` param (added in this remote's base action).
 *  * `cx` (contexts) contains an **encoded** array of entities, converted from `entities` param
 *     (included in each event/action). See below for notes on building the array and
 *     then encoding it.
 *  * `ue_px` (unstructured event params) contains **encoded** action params wrapped in an
 *     "unstructured event". See below for notes on converting action to an unstructured event
 *     and then encoding it.
 *  * `dtm` (device time) is a timestamp in milliseconds of when the action happened (when it
 *     was tracked). Use `time` param (from the base action).
 *  * `stm` (send time) is a timestamp in milliseconds of when the action was sent to the
 *     endpoint. Use current timestamp at the time of building the request.
 *     (Actions can be tracked locally and later actions that happened at different
 *     times can be sent together in batches. In such case the difference between `dtm` and `stm`
 *     is the time the action was waiting in the local cache before it was sent to the endpoint.)
 *
 * ### Attaching entities via the `cx` field
 *
 * `cx` (when not encoded, see note about encoding below) is a JSON object with two fields.
 * ```json
 * {
 *   "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1",
 *   "data": []
 * }
 * ```
 * `data` holds a flat array of any types of entities. To attach an entity to an event
 * format it as JSON structured as detailed below and add the resulting JSON object
 * to the array. You can add multiple entities of the same type (based on the same schema),
 * but only for some of them the data team will be able to make sense of it.
 *
 * To convert a thing representing an entity start with a JSON object with two fields.
 * `schema` holds a URI pointing to the Self-Describing JSON schema defining this entity.
 * Things modelled after Snowplow entities use it as their names. So the value of the
 * `schema` field should be the thing's full name.
 *
 * The second field is `data` and its value is a JSON object of the thing's fields.
 *
 * For example, let's imagine an entity representing a book, spec'd like this in Figment:
 * ```
 * thing book/1-0-0 {
 *     title : String
 *     pages : Integer
 * }
 * ```
 * JSON for "Howl's Moving Castle" (312 pages long) would look like this:
 * ```json
 * {
 *   "schema": "iglu:com.example/book/jsonschema/1-0-0",
 *   "data": {
 *     "title": "Howl's Moving Castle",
 *     "pages": 312
 *   }
 * }
 * ```
 *
 * ### Converting an action to an unstructured event
 *
 * `ue_px` (when not encoded, see not about encoding below) contains actual event params, but
 * wrapped in **two** layers of the schema/data structure you might start to recognise by now.
 *
 * On the top level `schema`'s value is always
 * `iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0`.
 * `data` holds the second layer.
 *
 * On the second layer set `schema` to action's full name. Similar to how it's done with
 * things representing entities, all events are spec'd as actions. The URI reference to the
 * Self-Describing JSON schema defining an event is used as action's name.
 *
 * Set `data` to a JSON object of the action's fields, but remove `action`, `time`, `eid` and
 * `entities` which you already set in different places in the JSON.
 *
 * For example, let's imagine we have an event representing reading a book, spec'd like this:
 * ```
 * action read/1-0-0 {
 *     start_page : Integer,
 *     end_page : Integer
 * }
 * ```
 * JSON for reading pages 9 through 23 would look like this:
 * ```json
 * {
 *   "schema": "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
 *   "data": {
 *     "schema": "iglu:com.example/read/jsonschema/1-0-0",
 *     "data": {
 *       "start_page": 9,
 *       "end_page": 23
 *     }
 *   }
 * }
 * ```
 *
 * ### Encoding `cx` and `ue_px` fields
 *
 * `cx` and `ue_px` fields don't actually hold the JSON objects described above. They hold
 * a Base64 encoded string of the JSON object. So after following the above sections to build
 * the JSON objects, don't set them as the value yet.
 *
 * First print the resulting JSON object to a string. Then encode the string with Base64. Set
 * the resulting encoded string as the corresponding field's value.
 *
 * If this description is not precise enough (because there are some options required by the
 * Base64 encoder on your platform, etc.) it's best to copy whatever the 1st party Snowplow SDK
 * for your platform is doing. Their SDK's are all open source.
 *
 * For example this is an implementation in the Snowplow SDK on the JVM:
 * https://github.com/snowplow/snowplow-java-tracker/blob/b6959231a0128f31003f2fdc6da334d3bbdea5a6/src/main/java/com/snowplowanalytics/snowplow/tracker/payload/TrackerPayload.java#L91
 * Looks like it uses UTF-8 encoding when serialising JSON to a string and then the
 * Base64 encoding uses a "single-line non-chunking" method.
 */
class SnowplowSource(
    private val httpClient: EclecticHttp,
    private val config: Config
) : FullResultSource, LimitedSource {

    companion object {
        val JSON_CONFIG = JsonConfig(PocketRemoteStyle.SNOWPLOW, true)
        const val PRODUCTION_COLLECTOR = "https://getpocket.com"
        const val DEV_COLLECTOR = "https://com-getpocket-prod1.mini.snplow.net"
        const val PRODUCTION_POST_PATH = "/t/e"
        const val DEV_POST_PATH = "/com.snowplowanalytics.snowplow/tp2"
    }

    /**
     * @param collector The domain hosting the collector endpoint to send events to.
     */
    class Config(internal val collector: String, internal val collectorPostPath: String, internal val appId: SnowplowAppId)
    
    var maxActions: Int = MAX_ACTIONS_DEFAULT
        set(value) {
            field = when {
                value <= 0 -> MAX_ACTIONS_DEFAULT
                value > MAX_ACTIONS_LIMIT -> MAX_ACTIONS_LIMIT
                else -> value
            }
        }

    var credentials: Credentials? = null
    
    override fun <T : Thing?> syncFull(thing: T, vararg actions: Action): SyncResult<T> {
        val supported = actions.filterIsInstance(SnowplowEvent::class.java)
        val (_, _, device, app) = credentials ?: throw RuntimeException("missing credentials")
        val result = SyncResult.Builder(thing, actions)
    
        for (batch in supported.chunked(maxActions)) {
            try {
                val collectorUrl = "${config.collector}/${config.collectorPostPath}"
                val request = SnowplowEndpoint.Request(collectorUrl, app, device)
    
                request.json = jsonObject {
                    put(Fields.SCHEMA, Schemas.PAYLOAD_DATA)
                    putArray("data").apply {
                        for (action in batch) {
                            add(payload(action, config.appId, device.locale))
                        }
                    }
                }

                SnowplowEndpoint.execute(request, httpClient)
    
                for (action in batch) {
                    result.action(action, Status.SUCCESS)
                }
    
            } catch (throwable: Throwable) {
                val snowplowApiException = unwrapSnowplowApiException(throwable)
                val status = when {
                    snowplowApiException == null -> {
                        // Can't be sure if this is a retryable error or not, so default to retryable.
                        Status.FAILED
                    }
                    snowplowApiException.httpStatusCode > 0 -> Status.IGNORED // REVIEW copied this off AdzerkSource, but should we retry in case of HTTP 500? and should we use FAILED_DISCARD instead of IGNORED?
                    else -> Status.FAILED
                }
                for (action in batch) {
                    result.action(action, status, throwable)
                }
            }
        }
        
        if (thing != null) {
            result.thing(
                Status.IGNORED,
                null,
                "${javaClass.simpleName} doesn't support syncing things"
            )
        }
        
        return result.build(Status.IGNORED)
    }

    override fun isSupported(syncable: Syncable) = syncable is SnowplowEvent
}

private fun payload(action: SnowplowEvent, appId: SnowplowAppId, lang: String) = jsonObject {
    // Constant values, same for every action.
    put(Fields.PLATFORM, SnowplowPlatform.MOB)
    put(Fields.TRACKER_VERSION, TRACKER_VERSION)
    put(Fields.EVENT_TYPE, EVENT_TYPE_UNSTRUCTURED)
    put(Fields.APP_ID, appId)
    put(Fields.BR_LANG, lang)

    // Values added by the source at the time of sending.
    put(Fields.SEND_TIME, Timestamp.now().millis().toString())

    // Values declared by the action.
    put(Fields.EVENT_ID, action._eid())
    put(Fields.CONTEXTS, action._entities().toSnowplowJson().encoded())
    put(Fields.EVENT, action.toSnowplowJson().encoded())
    put(Fields.DEVICE_TIME, (action.time() as? Timestamp)?.millis().toString())
}

private fun List<SnowplowEntity>.toSnowplowJson(): ObjectNode {
    val fields = this.map { it.toSnowplowJson() }
    
    return jsonObject {
        put(Fields.SCHEMA, Schemas.CONTEXTS)
        putArray(Fields.DATA).apply {
            addAll(fields)
        }
    }
}

private fun SnowplowEntity.toSnowplowJson(): ObjectNode {
    @Suppress("DEPRECATION") // We have to handle deprecated entities here.
    val schema = when (this) {
        is AdEntity_1_0_0 -> "iglu:com.pocket/ad/jsonschema/1-0-0"
        is ApiUserEntity_1_0_0 -> "iglu:com.pocket/api_user/jsonschema/1-0-0"
        is ApiUserEntity_1_0_1 -> "iglu:com.pocket/api_user/jsonschema/1-0-1"
        is ContentEntity_1_0_0 -> "iglu:com.pocket/content/jsonschema/1-0-0"
        is FeatureFlagEntity_1_0_0 -> "iglu:com.pocket/feature_flag/jsonschema/1-0-0"
        is ReportEntity_1_0_0 -> "iglu:com.pocket/report/jsonschema/1-0-0"
        is UiEntity_1_0_1 -> "iglu:com.pocket/ui/jsonschema/1-0-1"
        is UiEntity_1_0_2 -> "iglu:com.pocket/ui/jsonschema/1-0-2"
        is UiEntity_1_0_3 -> "iglu:com.pocket/ui/jsonschema/1-0-3"
        is UserEntity_1_0_0 -> "iglu:com.pocket/user/jsonschema/1-0-0"
        is UserEntity_1_0_1 -> "iglu:com.pocket/user/jsonschema/1-0-1"
        is RecommendationEntity_1_0_0 -> "iglu:com.pocket/recommendation/jsonschema/1-0-0"
        is SlateEntity_1_0_0 -> "iglu:com.pocket/slate/jsonschema/1-0-0"
        is SlateLineupEntity_1_0_0 -> "iglu:com.pocket/slate_lineup/jsonschema/1-0-0"
        else -> throw RuntimeException("${javaClass.simpleName}'s schema not defined.")
    }
    val data = toJson(JSON_CONFIG)

    return jsonObject {
        put(Fields.SCHEMA, schema)
        set(Fields.DATA, data)
    }
}

private fun Action.toSnowplowJson(): ObjectNode {
    @Suppress("DEPRECATION") // We have to handle deprecated events here.
    val schema = when (this) {
        is TrackAppBackground_1_0_0 -> "iglu:com.pocket/app_background/jsonschema/1-0-0"
        is TrackAppOpen_1_0_0 -> "iglu:com.pocket/app_open/jsonschema/1-0-0"
        is TrackContentOpen_1_0_0 -> "iglu:com.pocket/content_open/jsonschema/1-0-0"
        is TrackEngagement_1_0_0 -> "iglu:com.pocket/engagement/jsonschema/1-0-0"
        is TrackEngagement_1_0_1 -> "iglu:com.pocket/engagement/jsonschema/1-0-1"
        is TrackImpression_1_0_0 -> "iglu:com.pocket/impression/jsonschema/1-0-0"
        is TrackImpression_1_0_1 -> "iglu:com.pocket/impression/jsonschema/1-0-1"
        is TrackImpression_1_0_2 -> "iglu:com.pocket/impression/jsonschema/1-0-2"
        is TrackVariantEnroll_1_0_0 -> "iglu:com.pocket/variant_enroll/jsonschema/1-0-0"
        else -> throw RuntimeException("${javaClass.simpleName}'s schema not defined.")
    }
    val data = toJson(JSON_CONFIG)

    // Remove base action fields.
    data.remove("action")
    data.remove("time")
    data.remove("eid")
    data.remove("entities")

    return jsonObject { 
        put(Fields.SCHEMA, Schemas.UNSTRUCT_EVENT)
        set(Fields.DATA, jsonObject {
            put(Fields.SCHEMA, schema)
            set(Fields.DATA, data)
        })
    }
}

private fun jsonObject(body: ObjectNode.() -> Unit): ObjectNode {
    return Modeller.OBJECT_MAPPER.createObjectNode().apply(body)
}

private fun ObjectNode.put(fieldName: String, v: StringEnum) {
    put(fieldName, v.value)
}

private fun JsonNode.encoded(): String {
    return Modeller.OBJECT_MAPPER.writeValueAsBytes(this)
        .let { ByteString.of(*it) }
        .base64()
}
