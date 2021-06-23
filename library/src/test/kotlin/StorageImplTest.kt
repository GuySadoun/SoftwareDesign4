package il.ac.technion.cs.softwaredesign

import main.kotlin.MemorySecureStorage
import main.kotlin.Serializer
import main.kotlin.Storage
import main.kotlin.StorageImpl
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.nio.charset.Charset

class StorageImplTest {

    data class Entry(val arg: String)

    class EntrySerializer: Serializer<Entry> {
        override fun serialize(decoded: Entry): ByteArray = (decoded).arg.toByteArray(Charset.defaultCharset())
        override fun deserialize(encoded: ByteArray): Entry = Entry(encoded.toString(Charset.defaultCharset()))
    }

    // Using String instead of ByteArray, because ByteArray's "equals" is not contentEquals
    private val storageInstance: Storage<Entry> = StorageImpl(EntrySerializer(),
        MemorySecureStorage())

    @Test
    fun `Read should return null for non existing key`() =
        assertNull(storageInstance.read("Non Existing").join())

    @Test
    fun `Delete should fail for non existing key`() =
        assertFalse(storageInstance.delete("Non Existing").join())

    @Test
    fun `Update should fail for non existing key`() =
        assertFalse(storageInstance.update("Non Existing", Entry("other value")).join())

    @Test
    fun `Read should succeed for an existing key`() {
        assertTrue(storageInstance.create("key", Entry("value")).join())
        assertEquals(storageInstance.read("key").join(), Entry("value"))
    }

    @Test
    fun `Create should fail on existing key`() {
        assertTrue(storageInstance.create("key", Entry("value")).join())
        assertFalse(storageInstance.create("key", Entry("other value")).join())
    }

    @Test
    fun `Update succeeds for existing key`() {
        assertTrue(storageInstance.create("key", Entry("value")).join())
        assertTrue(storageInstance.update("key", Entry("other value")).join())
        assertEquals(storageInstance.read("key").join(), Entry("other value"))
    }

    @Test
    fun `Delete succeeds for existing key`() {
        assertTrue(storageInstance.create("key", Entry("value")).join())
        assertTrue(storageInstance.delete("key").join())
        assertNull(storageInstance.read("key").join())
    }
}