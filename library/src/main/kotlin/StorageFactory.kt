package main.kotlin

import java.util.concurrent.CompletableFuture

interface StorageFactory {
    fun <DataEntry> open(name: String, serializer: Serializer<DataEntry>): CompletableFuture<Storage<DataEntry>>
}