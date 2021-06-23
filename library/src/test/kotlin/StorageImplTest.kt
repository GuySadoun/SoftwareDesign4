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
        assertNull(storageInstance.Read("Non Existing").join())

    @Test
    fun `Delete should fail for non existing key`() =
        assertFalse(storageInstance.Delete("Non Existing").join())

    @Test
    fun `Update should fail for non existing key`() =
        assertFalse(storageInstance.Update("Non Existing", Entry("other value")).join())

    @Test
    fun `Read should succeed for an existing key`() {
        assertTrue(storageInstance.Create("key", Entry("value")).join())
        assertEquals(storageInstance.Read("key").join(), Entry("value"))
    }

    @Test
    fun `Create should fail on existing key`() {
        assertTrue(storageInstance.Create("key", Entry("value")).join())
        assertFalse(storageInstance.Create("key", Entry("other value")).join())
    }

    @Test
    fun `Update succeeds for existing key`() {
        assertTrue(storageInstance.Create("key", Entry("value")).join())
        assertTrue(storageInstance.Update("key", Entry("other value")).join())
        assertEquals(storageInstance.Read("key").join(), Entry("other value"))
    }

    @Test
    fun `Delete succeeds for existing key`() {
        assertTrue(storageInstance.Create("key", Entry("value")).join())
        assertTrue(storageInstance.Delete("key").join())
        assertNull(storageInstance.Read("key").join())
    }
}