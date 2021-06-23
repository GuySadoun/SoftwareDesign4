package main.kotlin

class SerializerImpl : Serializer<String>{
    override fun serialize(decoded: String): ByteArray = decoded.toByteArray()
    override fun deserialize(encoded: ByteArray): String = String(encoded)
}