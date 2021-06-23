package il.ac.technion.cs.softwaredesign.services

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.AccessRequest
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import java.util.concurrent.CompletableFuture

class RequestAccessManager @Inject constructor(private val dbRequestAccessHandler: DbRequestAccessHandler) {

    fun addAccessRequest(request: AccessRequest): CompletableFuture<Unit>{
        return dbRequestAccessHandler.addRequest(request)
    }

    fun isRequestForUsernameExists(username: String): CompletableFuture<Boolean>{
        return dbRequestAccessHandler.isRequestForUsernameExists(username)
    }

    fun getAllRequests(): CompletableFuture<List<AccessRequest>> {
        val requestsList = mutableListOf<AccessRequest>()
        var listCompletable = CompletableFuture.completedFuture(requestsList)

        return dbRequestAccessHandler.getNumberOfRequests()
            .thenCompose { numberOfRequests ->
                for (i in 0 until numberOfRequests) {
                    listCompletable = listCompletable
                        .thenCompose {
                            dbRequestAccessHandler.isActiveBySerialNumber(i).thenCompose { isActive ->
                                if (isActive){
                                    dbRequestAccessHandler.getRequestBySerialNumber(i)
                                        .thenApply { request ->
                                            requestsList.add(request!!)
                                            requestsList
                                        }
                                } else {
                                    CompletableFuture.completedFuture(requestsList)
                                }
                            }
                        }
                }
                listCompletable
            }.thenApply { mutableList -> ImmutableList.copyOf(mutableList) }
    }

    fun removeRequest(username: String): CompletableFuture<Unit>{
        return dbRequestAccessHandler.removeRequest(username)
    }
}