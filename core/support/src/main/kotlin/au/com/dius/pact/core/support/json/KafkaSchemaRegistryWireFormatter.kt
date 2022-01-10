package au.com.dius.pact.core.support.json

object KafkaSchemaRegistryWireFormatter {

    private const val MAGIC_BYTES_OFFSET = 5

    @JvmStatic
    fun removeMagicBytes(json: ByteArray?): ByteArray? = json?.drop(MAGIC_BYTES_OFFSET)?.toByteArray()

    @JvmStatic
    fun addMagicBytesToString(json: String?): String? {
        val encodedBytes = json?.let { addMagicBytes(it.encodeToByteArray()) } ?: return null
        return String(encodedBytes)
    }

    @JvmStatic
    fun addMagicBytes(bytes: ByteArray?): ByteArray {
        if(bytes == null || bytes.isEmpty())
            return ByteArray(0)

        return kafkaSchemaRegistryMagicBytes() + bytes
    }

    private fun kafkaSchemaRegistryMagicBytes(): ByteArray {
        val bytes = (0..3).map { 0x00.toByte() } + 0x01.toByte()
        return bytes.toByteArray()
    }
}
