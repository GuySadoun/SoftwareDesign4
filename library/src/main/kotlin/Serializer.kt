package main.kotlin

/**
 * An interface to serialize and deserialize entries into and from ByteArray
 */
interface Serializer<DataEntry> {
    fun serialize(decoded: DataEntry): ByteArray
    fun deserialize(encoded: ByteArray): DataEntry
}