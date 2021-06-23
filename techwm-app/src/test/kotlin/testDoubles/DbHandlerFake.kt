package testDoubles

import library.interfaces.IDbHandler
import java.util.concurrent.CompletableFuture

class DbHandlerFake : IDbHandler {
    private val dbDictionary: MutableMap<String, String> = mutableMapOf()

    override fun write(key: String, value: String): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync { dbDictionary[key] = value }
    }

    override fun read(key: String): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync { dbDictionary[key] }
    }

    fun clearDatabase() {
        dbDictionary.clear()
    }
}