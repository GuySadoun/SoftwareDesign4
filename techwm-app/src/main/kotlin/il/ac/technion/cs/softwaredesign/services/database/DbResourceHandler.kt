package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.IdAlreadyExistException
import library.DbFactory
import java.util.concurrent.CompletableFuture

class DbResourceHandler @Inject constructor(databaseFactory: DbFactory) {
    companion object {
        const val serialNumberSeparator = "-"
        const val AvailableSymbol = "1"
        const val UnavailableSymbol = "0"
    }

    private val dbSerialNumberToIdHandler by lazy { databaseFactory.open(DbDirectoriesPaths.SerialNumberToId) }
    private val dbIdToResourceInfoHandler by lazy { databaseFactory.open(DbDirectoriesPaths.IdToResourceName) }

    /**
     * Get resource by id from DB if id exists
     *
     * @param id
     * @return CompletableFuture of the info if id exists in DB, else CompletableFuture<null>
     */
    fun getResourceById(id: String) : CompletableFuture<ResourceInfo?> {
        return dbIdToResourceInfoHandler.read(id).thenApply { resourceInfoString ->
            if (resourceInfoString.isNullOrEmpty())
                null
            else {
                val isAvailable = resourceInfoString[0] == '1'
                val splitResourceInfoString = resourceInfoString.split(serialNumberSeparator)

                val serialNumber = splitResourceInfoString[0]
                    .substring(1) // emit availability symbol
                    .toInt()
                val resourceName = splitResourceInfoString[1]

                ResourceInfo(isAvailable, serialNumber, resourceName)
            }
        }
    }

    /**
     * Add hardware resource to DB
     *
     * @param id
     * @param name
     * @return CompletableFuture of the action
     */
    fun addHardwareResource(id: String, name: String): CompletableFuture<Unit> {
        return getResourceById(id).thenApply { resource ->
            if (resource != null) {
                throw IdAlreadyExistException()
            }
        }.thenCompose { dbSerialNumberToIdHandler.read("size") }.thenCompose { flow ->
                val serialNumber: Int = flow?.toInt() ?: 0

                dbSerialNumberToIdHandler.write(serialNumber.toString(), id).thenCompose {
                    dbSerialNumberToIdHandler.write("size", (serialNumber + 1).toString()).thenCompose {
                        dbIdToResourceInfoHandler.write(id, "$AvailableSymbol$serialNumber$serialNumberSeparator$name") }
                    }
            }
    }

    /**
     * Get resource id by serial number from DB if id exists
     *
     * @param serialNumber
     * @return CompletableFuture of the info if id exists in DB, else CompletableFuture<null>
     */
    fun getResourceIdBySerialNumber(serialNumber: Int): CompletableFuture<String?> {
        return dbSerialNumberToIdHandler.read(serialNumber.toString())
    }

    /**
     * Get resources size
     *
     * @return CompletableFuture of how many resources exist in DB
     */
    fun getResourcesSize(): CompletableFuture<Int> {
        return dbSerialNumberToIdHandler.read("size")
            .thenApply { size -> size?.toInt() ?: 0 }
    }

    /**
     * Mark resource as available
     *
     * @param id
     * @return CompletableFuture of the action
     */
    fun markResourceAsAvailable(id: String): CompletableFuture<Unit> {
        return getResourceById(id).thenCompose { resource ->
            val serialNumber = resource!!.serialNumber
            val name = resource.name
            dbIdToResourceInfoHandler.write(id, "$AvailableSymbol$serialNumber$serialNumberSeparator$name")
        }
    }

    /**
     * Mark resource as unavailable
     *
     * @param id
     * @return CompletableFuture of the action
     */
    fun markResourceAsUnavailable(id: String): CompletableFuture<Unit> {
        return getResourceById(id).thenCompose { resource ->
            val serialNumber = resource!!.serialNumber
            val name = resource.name
            dbIdToResourceInfoHandler.write(id, "$UnavailableSymbol$serialNumber$serialNumberSeparator$name")
        }
    }
}

data class ResourceInfo (val isAvailable: Boolean, val serialNumber: Int, val name: String)