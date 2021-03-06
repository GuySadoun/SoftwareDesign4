package main.kotlin

import java.util.concurrent.CompletableFuture

/**
 * An interface to store pairs of [String] keys and [DataEntry]s values in a persistent DB.
 */
interface Storage<DataEntry> {

    /**
     * Store a new entry with key [key] and value [dataEntry] in the DB, only if the key does not already exist.
     *
     * @return true if the operation succeeded, and false otherwise (if the key already existed)
     */
    fun create(key: String, dataEntry: DataEntry) : CompletableFuture<Boolean>

    /**
     * Fetch the value that is stored in the DB and associated with [key], only if the key exists.
     *
     * @return the entry on success, and null if the key does not exist
     */
    fun read(key: String) : CompletableFuture<DataEntry?>

    /**
     * Change the value associated with [key] in the DB.
     * The previous value is removed and no longer exists.
     *
     * @return true if the operation succeeded, and false if the key does not exist
     */
    fun update(key: String, dataEntry: DataEntry) : CompletableFuture<Boolean>

    /**
     * Delete the value that is associated with [key] in the DB.
     *
     * @return true if the operation succeeded, and false if the key does not exist
     */
    fun delete(key: String) : CompletableFuture<Boolean>

    /**
     * Write the value to DB, override it's value or create a new one
     *
     * @param key
     * @param dataEntry
     * @return
     */
    fun write(key: String, dataEntry: DataEntry) : CompletableFuture<Unit>
}