package main.kotlin

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture
import javax.inject.Inject


class StorageImpl<DataEntry> @Inject constructor(private val serializer: Serializer<DataEntry>,
                                                 private val storage: SecureStorage) : Storage<DataEntry>
{
    override fun create(key: String, dataEntry: DataEntry) : CompletableFuture<Boolean> {
        return internalRead(key).thenCompose {
            if (it != null) {
                CompletableFuture.completedFuture(false)
            }
            else {
                storage.write(key.toByteArray(), serializer.serialize(dataEntry)).thenApply { true }
            }
        }
    }

    override fun read(key: String): CompletableFuture<DataEntry?> {
        return internalRead(key)
    }

    override fun update(key: String, dataEntry: DataEntry): CompletableFuture<Boolean> {
        return internalRead(key).thenCompose {
            if (it == null) {
                CompletableFuture.completedFuture(false)
            }
            else {
                storage.write(key.toByteArray(), serializer.serialize(dataEntry)).thenApply { true }
            }
        }
    }

    override fun delete(key: String): CompletableFuture<Boolean> {
        return internalRead(key).thenCompose {
            if (it == null) {
                CompletableFuture.completedFuture(false)
            }
            else {
                storage.write(key.toByteArray(), ByteArray(0)).thenApply { true }
            }
        }
    }

    private fun internalRead(key: String): CompletableFuture<DataEntry?> {
        return storage.read(key.toByteArray()).thenApply {
            if (it == null || it.isEmpty()) {
                null
            }
            else {
                val result = serializer.deserialize(it)
                (result as? DataEntry) ?: throw IllegalStateException()
            }
        }
    }

    override fun write(key: String, dataEntry: DataEntry): CompletableFuture<Unit> {
        return create(key, dataEntry).thenApply { created ->
            if (!created) {
                update(key, dataEntry).thenApply { updated ->
                    if (!updated) throw Exception("Should not be thrown")
                }
            }
        }
    }
}