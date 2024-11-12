package com.pocket.sdk.api.endpoint

import com.fasterxml.jackson.databind.node.ObjectNode
import com.pocket.sdk.network.eclectic.EclecticHttp
import org.apache.commons.lang3.exception.ExceptionUtils

object SnowplowEndpoint {
    /**
     * Executes a request to Snowplow and checks response status.
     *
     * @param request The contents of the request
     * @param httpClient The http client to invoke the request on
     */
    fun execute(request: Request, httpClient: EclecticHttp) {
        try {
            val httpRequest = httpClient.buildRequest(request.endpointUrl)
                .setHeader("User-Agent", Endpoint.userAgent(request.app, request.device))
            if (request.json != null) {
                httpRequest.json = request.json.toString()
            }

            val response = httpClient.post(httpRequest, null)
            if (response.statusCode != 200) {
                throw SnowplowApiException(httpStatusCode = response.statusCode)
            }
    
        } catch (snowplowApiException: SnowplowApiException) {
            throw snowplowApiException
        } catch (throwable: Throwable) {
            throw SnowplowApiException(cause = throwable)
        }
    }
    
    class Request(
        val endpointUrl: String,
        val app: AppInfo,
        val device: DeviceInfo,
        var json: ObjectNode? = null,
    )
}

data class SnowplowApiException(
    override val cause: Throwable? = null,
    val httpStatusCode: Int = 0
) : Exception(cause)

fun unwrapSnowplowApiException(t: Throwable): SnowplowApiException? {
    val i = ExceptionUtils.indexOfType(t, SnowplowApiException::class.java)
    return if (i >= 0) ExceptionUtils.getThrowables(t)[i] as? SnowplowApiException else null
}
