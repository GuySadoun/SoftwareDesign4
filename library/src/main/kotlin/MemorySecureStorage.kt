package main.kotlin

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

class MemorySecureStorage : SecureStorage {
    var storage = HashMap<String, ByteArray>()

    override fun read(key: ByteArray) : CompletableFuture<ByteArray?> {
        return CompletableFuture.completedFuture(storage[key.toString(Charsets.UTF_8)])
    }

    override fun write(key: ByteArray, value: ByteArray) : CompletableFuture<Unit> {
        storage[key.toString(Charsets.UTF_8)] = value
        return CompletableFuture.completedFuture(Unit)
    }
}

class MemorySecureStorageFactory : SecureStorageFactory {
    override fun open(name: ByteArray): CompletableFuture<SecureStorage> {
        return CompletableFuture.completedFuture(MemorySecureStorage())
    }

}