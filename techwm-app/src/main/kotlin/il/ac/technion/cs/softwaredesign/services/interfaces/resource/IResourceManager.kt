package il.ac.technion.cs.softwaredesign.services.interfaces.resource

import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import java.util.concurrent.CompletableFuture

interface IResourceManager {
    fun isIdExist(id: String): CompletableFuture<Boolean>
    fun attachHardwareResource(id: String, name: String): CompletableFuture<Unit>
    fun getResourceName(id: String): CompletableFuture<String?>
    fun getAttachedResources(n: Int): CompletableFuture<List<String>>
    fun verifyResource(id: String): CompletableFuture<Class<out GeneralResource>>
    fun allocateResource(id: String): CompletableFuture<GeneralResource>
    fun isAvailable(id: String): CompletableFuture<Boolean>
    fun releaseResource(resource: GeneralResource): CompletableFuture<Unit>
}
