package il.ac.technion.cs.softwaredesign

import java.util.concurrent.CompletableFuture

/**
 * A message sent by [fromUser]
 */
data class Message(val id: String, val fromUser: String, val message: String)

typealias Inbox = Map<String, List<Message>>

/**
 * An access request is made by a non-registered user, to register the system. This request is visible by all
 * administrators in the system.
 */
interface AccessRequest {
    val requestingUsername: String
    val reason: String

    /**
     * Approve the request.
     */
    fun approve(): CompletableFuture<Unit>

    /**
     * Decline the request
     */
    fun decline(): CompletableFuture<Unit>
}

/**
 * This is the main class implementing TechWM Clients, the client layer interacting with the TechWM application.
 */
open class TechWorkloadUserClient {
    /**
     * Login with a given password. A successfully logged-in user is considered "online". If the user is already
     * logged in, this is a no-op.
     *
     * This is a *create* operation.
     *
     * @throws IllegalArgumentException If the password was wrong or the user is not yet registered.
     */
    fun login(password: String): CompletableFuture<Unit> = TODO("Implement me!")

    /**
     * Log out of the system. After logging out, a user is no longer considered online.
     *
     * This is a *delete* operation.
     *
     * @throws IllegalArgumentException If the user was not previously logged in.
     */
    fun logout(): CompletableFuture<Unit> = TODO("Implement me!")

    /**
     * Queue resources for a job.
     *
     * This is a *create* operation
     *
     * @throws PermissionException If the user is not logged in.
     * @throws IllegalArgumentException If the job could not be submitted, according to permission or account policy.
     */
    fun submitJob(jobName: String, resources: List<String>): CompletableFuture<AllocatedJob> = TODO("Implement me!")

    /**
     * As a new user, request access to the system for a given [username] and [password].
     * This notification will be posted to all active administrators, viewable by [TechWorkloadAdminClient.accessRequests].
     * The future resolves when the request is sent. If a request is accepted, the user can then [login] freely.
     *
     * This is a *create* operation
     *
     * @throws IllegalArgumentException If the user already requested access and the previous request was not
     * resolved (accepted/denied)
     */
    fun requestAccessToSystem(username: String, password: String, reason: String): CompletableFuture<Unit> = TODO("Implement me!")

    /**
     * Get online users, possible filtering by a [PermissionLevel]. If [PermissionLevel] is `null`, show all users.
     *
     * This is a *read* operation.
     *
     * @throws PermissionException If the user is not logged in.
     * @return A list of user IDs which are currently online.
     */
    fun onlineUsers(permissionLevel: PermissionLevel? = null): CompletableFuture<List<String>> = TODO("Implement me!")

    /**
     * Get messages currently in your inbox from other users.
     *
     * This is a *read* operation.
     *
     * @return A mapping from usernames to lists of messages (conversations), sorted by time of sending.
     * @throws PermissionException If the user is not logged in.
     */
    fun inbox(): CompletableFuture<Inbox> = TODO("Implement me!")

    /**
     * Send a message to a username [toUsername].
     *
     * This is a *create* operation.
     *
     * @throws PermissionException If the user is not logged in.
     * @throws IllegalArgumentException If the target user does not exist, or message contains more than 120 characters.
     */
    fun sendMessage(toUsername: String, message: String): CompletableFuture<Unit> = TODO("Implement me!")

    /**
     * Delete a message from your inbox.
     *
     * This is a *delete* operation.
     *
     * @throws PermissionException If the user is not logged in.
     * @throws IllegalArgumentException If a message with the given [id] does not exist
     */
    fun deleteMessage(id: String): CompletableFuture<Unit> = TODO("Implement me!")
}

class TechWorkloadAdminClient: TechWorkloadUserClient() {
    /**
     * View all access requests in the system.
     *
     * This is a *read* operation.
     *
     * @throws PermissionException If the user is not logged in.
     */
    fun accessRequests(): CompletableFuture<List<AccessRequest>> = TODO("Implement me!")
}

/**
 * A factory for creating user clients.
 */
interface TechWorkloadClientFactory {
    /**
     * Get an instance of a [TechWorkloadUserClient] for a given username.
     */
    fun get(username: String): TechWorkloadUserClient
}