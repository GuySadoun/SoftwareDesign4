package main.kotlin

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture
import javax.inject.Inject


class StorageImpl<DataEntry> @Inject constructor(private val serializer: Serializer<DataEntry>,
                                                 private val storage: SecureStorage) : Storage<DataEntry>
{
    override fun Create(key: String, dataEntry: DataEntry) : CompletableFuture<Boolean> {
        return internalRead(key).thenCompose {
            if (it != null) {
                CompletableFuture.completedFuture(false)
            }
            else {
                storage.write(key.toByteArray(), serializer.serialize(dataEntry)).thenApply { true }
            }
        }
    }

    override fun Read(key: String): CompletableFuture<DataEntry?> {
        return internalRead(key)
    }

    override fun Update(key: String, dataEntry: DataEntry): CompletableFuture<Boolean> {
        return internalRead(key).thenCompose {
            if (it == null) {
                CompletableFuture.completedFuture(false)
            }
            else {
                storage.write(key.toByteArray(), serializer.serialize(dataEntry)).thenApply { true }
            }
        }
    }

    override fun Delete(key: String): CompletableFuture<Boolean> {
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
}