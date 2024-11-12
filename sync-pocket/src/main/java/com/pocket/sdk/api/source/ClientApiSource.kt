package com.pocket.sdk.api.source

import com.pocket.sdk.api.endpoint.ClientApiHandler
import com.pocket.sdk.api.endpoint.Credentials
import com.pocket.sdk.api.endpoint.Endpoint
import com.pocket.sdk.api.generated.PocketRemoteStyle
import com.pocket.sdk.network.eclectic.EclecticHttp
import com.pocket.sync.action.Action
import com.pocket.sync.source.FullResultSource
import com.pocket.sync.source.JsonConfig
import com.pocket.sync.source.LimitedSource
import com.pocket.sync.source.protocol.graphql.GraphQlSource
import com.pocket.sync.spec.Syncable
import com.pocket.sync.thing.Thing


/**
 * The Client API is the start of the next generation of API development at Pocket. It is a Federated Apollo GraphQL Gateway that clients can connect to and receive standard representations of objects no matter where they may have come from under the hood.
 *
 * This readme assumes a basic understanding of GraphQL which can be found at <https://graphql.org/learn/>
 *
 * This is a work in progress, a long term plan can be found here https://docs.google.com/document/d/10UrIORsEblWd_hj9WyF71aFOVcHcJ56YKgwWisZtBac/edit
 *
 * **Apollo Federated GraphQL Gateway???**
 * That's right, that big term means that the service is GraphQL endpoint that understands Pocket's backend systems and will delegate requests to the appropriate backend service.
 *
 * *[Apollo](https://www.apollographql.com/) here just is the company that has developed the set of tools and business around helping developer teams accomplish this*.
 *
 * For instance you can have a GraphQL query that looks like the following:
 *
 * ```graphql
 * query Query {
 *   getParserItemByUrl(url: "https://getpocket.com/explore/item/the-color-you-should-never-paint-your-front-door-according-to-real-estate-agents") {
 *    itemId
 *    title
 *    syndicatedArticle {
 *      slug
 *      publisher {
 *        name
 *      }
 *    }
 *   }
 * }
 * ```
 *
 * In this Federated GraphQL world, the Gateway will do the following:
 *
 * 1. Go to the Parser service to get an item
 * 2. Go to the Syndication service to get any syndicated data if it is syndicated
 *
 * In the days of future past, this would have been multiple mysql calls syncronously in the Web repo, but the gateway allows to asynchronously make the requests to our various services and not worry about where and how the data exists.
 *
 * Apps don't actually need to know about this magic, since to them, it is all one united API.
 *
 * ## Can anyone push to the Pocket Graph?
 *
 * Yep! Anyone at Pocket that maintains a micro service will be able to push their service to the wider Pocket Graph. We will ask however that all additions to the graph be discussed with a PR to the graph repository first.
 *
 * 🚧*Instructions on how you can push to the graph to come at a later date*.🚧
 *
 * ## How Can Clients Use It?
 *
 * Clients can access the graph by making a regular Pocket request (like v3) to api.getpocket.com/graphql
 *
 * You will then include a GraphQL post body.
 *
 * Taking the example above it could like like this:
 *
 * ```bash
 * curl 'https://api.getpocket.com/graphql' --data-raw '{"query":"query Query($getParserItemByUrlUrl: String!) {\n  getParserItemByUrl(url: $getParserItemByUrlUrl) {\n    itemId\n    title\n    syndicatedArticle {\n      slug\n      publisher {\n        name\n      }\n    }\n  }\n}\n","variables":{"getParserItemByUrlUrl":"https://getpocket.com/explore/item/the-color-you-should-never-paint-your-front-door-according-to-real-estate-agents"},"operationName":"Query"}'
 * ```
 *
 * Apollo also offers a lot of out of the box libraries to assist with code generation, query caching, and much more.
 *
 * * [iOS](https://www.apollographql.com/docs/ios/)
 * * [Android](https://www.apollographql.com/docs/android/)
 * * [React/JS](https://www.apollographql.com/docs/react/)
 *
 * ### Rolling your own client
 *
 * If you do roll your own client it is requested that you add the following headers:
 *
 * * `apollographql-client-name`  - The name of your client
 * * `apollographql-client-version` - The version of your client
 *
 * These headers will populate into Apollo Studio and allow the Backend and other teams the ability to see which fields are actively being used and which fields are safe for deprecation.
 *
 * If you are using an Apollo client these headers can be set when you initialize the client. See <https://www.apollographql.com/docs/studio/client-awareness/#using-apollo-server-and-apollo-client> for more details.
 *
 * ## But What About Auth?
 *
 * Today clients should pass their auth parameters as they currently do to the endpoint. (See details in {V3}) The endpoint in the Web repo will then encode the authenticated user into a JWT token that will be passed to all the various downstream services. The downstream services can then verify that JWT token and perform any user specific actions that they need to.
 *
 * *In the far future we eventually hope clients will be able to pass a signed JWT auth token from auth.getpocket.com direct to the graphql service bypassing our current authentication strategies.*
 *
 * ## Is there any Easy Playground?
 *
 * Why yes there is! However it has some caveats.
 *
 * First ask #support-backend about getting access to <https://studio.apollographql.com>. In here you will find an entire graphql playground that has our Pocket graph and an interactive explorer that will let you test and play with queries.
 *
 * BUT here's the caveat. This studio connects directly to the underlying service that lies beneath api.getpocket.com/graphql .   This means that you can not do any auth based GraphQL queries because it will not apply the necessary JWT header.
 *
 * ## Cool Cool. But really. Why?
 *
 * Currently Pocket has an issue where EVERYTHING we do with our API must occur through the Web repository. This is because Web hosts our central rules for rate limiting, authentication, consumer keys, analytics, decoration and much much more. As Pocket grows it's becoming harder and harder for other development teams to make API's and add to our existing `/v3` API without requiring the Backend team to perform some kind of proxy layer.
 *
 * This solution allows us as an org to create 1 `graphql` proxy layer and then allow other teams to easily push to a Graph that can then be immediately used. It also provides the decoration for objet types across Pocket.
 *
 * A good example is a Recommendations service. Currently the recommendation service may only return an Item Id, but our clients need a lot more information to properly display it to a user. Now this recs service can push a Graph that returns just the Item Id, but the Gateway will know that the Item Id means something much more and will transform it into the larger Pocket Item model that our clients expect automatically.
 *
 * And best of all not all of our clients consume all the information that we return from our various endpoints. GraphQL will allow the client consumers to be very specific about the data they want returned to them, becoming much more data efficient.
 *
 * For a deep dive on why, check out https://docs.google.com/document/d/1odBd18Z3_NVa6meWDHcuV2GIAoUnjHu1pIecavWnXlY/edit#heading=h.2m29332mzjiz
 *
 * ## Where can I find our Current GraphQL Schema?
 *
 * Check out {ClientApiSlice} for details on what has been moved over so far.
 *
 * ## Name Changes
 *
 * With the move to Client API, we'll also be updating our naming/casing conventions from snake_case to camelCase.
 * For fields that have had these tweaks, aliases have been added. For example, see the field aliases on {Item.item_id}.
 *
 * If apps need to add an adapter in the meantime, just a heads up is that GraphQL allows clients to dictate their response names and structure.
 *
 * So for instance you could alias names like the following:
 *
 * ```graphql
 * query Query($getParserItemByUrlUrl: String!) {
 *   item: getParserItemByUrl(url: $getParserItemByUrlUrl) {
 *    item_id: itemId
 *    title
 *    syndicated_article: syndicatedArticle {
 *      slug
 *      publisher {
 *        name
 *      }
 *    }
 *   }
 * }
 * ```
 *
 * and this will output as
 *
 * ```json
 * {
 *   "data": {
 *     "item": {
 *       "item_id": "3071805238",
 *       "title": "The Color You Should Never Paint Your Front Door",
 *       "syndicated_article": {
 *         "slug": "the-color-you-should-never-paint-your-front-door-according-to-real-estate-agents",
 *         "publisher": {
 *           "name": "Apartment Therapy"
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 */
class ClientApiSource(httpClient: EclecticHttp) : FullResultSource, LimitedSource {

    companion object {
        val JSON_CONFIG = JsonConfig(PocketRemoteStyle.CLIENT_API, false)
    }

    private val handler = ClientApiHandler(httpClient)
    private val graphQl = GraphQlSource(handler, JSON_CONFIG, apqSupported = false)

    override fun <T : Thing> syncFull(thing: T?, vararg actions: Action) = graphQl.syncFull(thing, *actions)

    @Synchronized
    fun setCredentials(credentials: Credentials?) {
        handler.credentials = credentials
    }

    override fun isSupported(syncable: Syncable) = syncable.remote()?.style == PocketRemoteStyle.CLIENT_API
}
