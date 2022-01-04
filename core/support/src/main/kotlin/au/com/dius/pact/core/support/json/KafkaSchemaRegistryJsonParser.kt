package au.com.dius.pact.core.support.json

import java.io.InputStream
import java.io.Reader

object KafkaSchemaRegistryJsonParser {


    @Throws(JsonException::class)
    @JvmStatic
    fun parseString(json: String): JsonValue {
        if (json.length > 5)
            return JsonParser.parseString(removeMagicBytes(json))
        return JsonParser.parseString(json)
    }

    private fun removeMagicBytes(contentIncludingMagicBytes: String): String {
        val contentOnly = contentIncludingMagicBytes.drop(5)
        return contentOnly
    }

    @Throws(JsonException::class)
    @JvmStatic
    fun parseStream(json: InputStream): JsonValue {
        return JsonParser.parseStream(json)
    }

    @Throws(JsonException::class)
    @JvmStatic
    fun parseReader(reader: Reader): JsonValue {
        return JsonParser.parseReader(reader)
    }
}
