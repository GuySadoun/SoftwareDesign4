package testDoubles

import main.kotlin.Storage
import java.util.concurrent.CompletableFuture

class StorageFake : Storage<String> {
    private val dbDictionary: MutableMap<String, String> = mutableMapOf()

    override fun write(key: String, dataEntry: String): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync { dbDictionary[key] = dataEntry }
    }

    override fun read(key: String): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            if (dbDictionary[key].isNullOrEmpty())
                null
            else
                dbDictionary[key]
        }
    }

    fun clearDatabase() {
        dbDictionary.clear()
    }

    override fun create(key: String, dataEntry: String): CompletableFuture<Boolean> {
        throw Exception("not implemented")
    }

    override fun update(key: String, dataEntry: String): CompletableFuture<Boolean> {
        throw Exception("not implemented")
    }

    override fun delete(key: String): CompletableFuture<Boolean> {
        throw Exception("not implemented")
    }
}