package il.ac.technion.cs.softwaredesign.services

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.execution.ExecutionService
import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import il.ac.technion.cs.softwaredesign.services.database.DbResourceHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.resource.IResourceManager
import java.lang.Integer.min

import java.util.concurrent.CompletableFuture

/**
 * Resource manager of the system
 *
 * @property dbResourceHandler resource database handler
 * @property mExecService execution service of the system
 * @constructor Create empty Resource manager
 */
class ResourceManager @Inject constructor(
    private val dbResourceHandler: DbResourceHandler,
    private val mExecService: ExecutionService
    ): IResourceManager {

    /**
     * return whether a resource with the id exists
     *
     * @param id of the resource
     * @return CompletableFuture with the result
     */
    override fun isIdExist(id: String): CompletableFuture<Boolean> {
        return dbResourceHandler.getResourceById(id).thenApply { resourceInfo ->
            resourceInfo != null
        }
    }

    /**
     * Attach new resource resource to the system
     *
     * @param id of the resource
     * @param name of the resource
     * @return CompletableFuture of the action
     */
    override fun attachHardwareResource(id: String, name: String): CompletableFuture<Unit> {
        return dbResourceHandler.addHardwareResource(id, name)
    }

    /**
     * Get resource name by its id
     *
     * @param id of the resource
     * @return CompletableFuture of the name, null (inside the completable) with the id does not exists in the system
     */
    override fun getResourceName(id: String): CompletableFuture<String?> {
        return dbResourceHandler.getResourceById(id).thenApply { resourceInfo -> resourceInfo?.name }
    }

    /**
     * Get list of the first n available resources sorted by attached date
     *
     * @param n number of resources to return
     * @return CompletableFuture of the list
     */
    override fun getAttachedResources(n: Int): CompletableFuture<List<String>> {
        val resourcesList = mutableListOf<String>()
        var listCompletable = CompletableFuture.completedFuture(resourcesList)

        return dbResourceHandler.getResourcesSize().thenApply { size -> min(size, n) }
            .thenCompose { numberOfElementsToTake ->
                for (i in 0 until numberOfElementsToTake) {
                    listCompletable = listCompletable
                        .thenCompose { dbResourceHandler.getResourceIdBySerialNumber(i) }
                        .thenApply { resourceId ->
                            resourcesList.add(resourceId!!)
                            resourcesList
                        }
                }
                listCompletable
            }.thenApply { mutableList -> ImmutableList.copyOf(mutableList) }
    }

    /**
     * Verify resource with the system's execution service
     *
     * @param id of the resource
     * @return
     */
    override fun verifyResource(id: String): CompletableFuture<Class<out GeneralResource>> {
        return mExecService.verifyResource(id)
    }

    /**
     * Allocate resource with the system's execution service and mark it as unavailable
     *
     * @param id of the resource
     * @return
     */
    override fun allocateResource(id: String): CompletableFuture<GeneralResource> {
        return dbResourceHandler.markResourceAsUnavailable(id).thenCompose { mExecService.allocateResource(id) }
    }

    /**
     * Release resource with the system's execution service and mark it as available
     *
     * @param resource - [GeneralResource] object of the resource
     * @return
     */
    override fun releaseResource(resource: GeneralResource): CompletableFuture<Unit> {
        return dbResourceHandler.markResourceAsAvailable(resource.id).thenCompose { mExecService.releaseResource(resource) }
    }

    /**
     * return whether the resource is available
     *
     * @param id of the resource
     * @return CompletableFuture of the result
     */
    override fun isAvailable(id: String): CompletableFuture<Boolean> {
        return dbResourceHandler.getResourceById(id).thenApply { resourceInfo ->
            resourceInfo?.isAvailable ?: false
        }
    }
}