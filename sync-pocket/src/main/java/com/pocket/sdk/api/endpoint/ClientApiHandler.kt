package com.pocket.sdk.api.endpoint

import com.fasterxml.jackson.databind.node.ObjectNode
import com.pocket.sdk.network.eclectic.EclecticHttp
import com.pocket.sdk.network.eclectic.EclecticHttpRequest
import com.pocket.sync.source.protocol.graphql.GraphQlSource
import com.pocket.util.java.JsonUtil
import org.apache.commons.lang3.RandomStringUtils
import java.io.InputStream

private const val CLIENT_API_ENDPOINT_URL = "https://api.getpocket.com/graphql" // During debugging/testing, "https://client-api.getpocket.com" may also be helpful, it skips the web repo proxy. However this should only be used for debugging and not production.

class ClientApiHandler(val httpClient: EclecticHttp) : GraphQlSource.HttpHandler {

    var credentials: Credentials? = null

    override fun execute(request: ObjectNode, response: (body: InputStream?, httpStatus: Int?, error: Throwable?) -> Unit) {

        val app = credentials?.app
        val device = credentials?.device

        val httpRequest: EclecticHttpRequest = httpClient.buildRequest(CLIENT_API_ENDPOINT_URL)

        httpRequest.setHeader("apollographql-client-name", "Android")
                .setHeader("apollographql-client-version", app?.productVersion)
                .setHeader("User-Agent", Endpoint.userAgent(app, device))

        request.put("locale_lang", device?.locale)
        request.put("consumer_key", app?.consumerKey)

        credentials?.guid?.also { request.put("guid", it) }

        credentials?.userAccessToken?.also {
            request.put("access_token", it)
            val timestamp = System.currentTimeMillis().toString()
            val nonce = RandomStringUtils.randomAlphanumeric(16)
            request.put("oauth_timestamp", timestamp)
            request.put("oauth_nonce", nonce)
            request.put("sig_hash", Endpoint.hash(timestamp, nonce, it))
        }

        httpRequest.json = request.toString()

        try {
            httpClient.post(httpRequest) { stream, r ->
                val body : InputStream? = try { stream.inputStream() } catch (t: Throwable) { null }
                response(body, r.statusCode, null)
            }
        } catch (e: Throwable) {
            response(null, null, e)
        }
    }
}