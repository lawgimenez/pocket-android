package com.pocket.sync.source

/**
 * Configuration for parsing to / from Json
 * @param remote [RemoteStyle] to use, which denotes what aliases, if any, to use.
 * @param supportsIntEnums, whether the source supports enums as integers.
 *  The v3 api used integers to denote some enum values, such as Item status, whereas
 *  our graphql endpoint uses the String name of the status instead.
 */
class JsonConfig(val remote: RemoteStyle?, val supportsIntEnums: Boolean)