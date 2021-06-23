package il.ac.technion.cs.softwaredesign.services

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbTokenHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenGenerator
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenManager
import java.util.concurrent.CompletableFuture

class TokenManager @Inject constructor (private val mTokenHandler: IDbTokenHandler, private val mTokenGenerator: ITokenGenerator):
    ITokenManager {

    /**
     * Create or replace user token
     *
     * @param username
     * @return CompletableFuture with the new token
     */
    override fun createOrReplaceUserToken(username: String): CompletableFuture<String> {
        val token = generateToken()

        return mTokenHandler.setOrReplaceTokenToUsername(token, username)
            .thenApply { token }
    }

    /**
     * Get username by token if exists
     *
     * @param token of the user
     * @return CompletableFuture with the result or null if user does not exists
     */
    override fun getUsernameByTokenIfExists(token: String): CompletableFuture<String?> {
        return mTokenHandler.getUsernameByToken(token)
    }

    /**
     * Invalidate user token
     *
     * @param username
     * @return CompletableFuture of the action
     */
    override fun invalidateUsernameToken(username: String): CompletableFuture<Unit> {
        return mTokenHandler.deleteUserPreviousTokenIfExist(username)
    }

    private fun isTokenExists(token: String): CompletableFuture<Boolean> {
        return getUsernameByTokenIfExists(token).thenApply { it != null }
    }


    private fun generateToken(): String {
        var token = mTokenGenerator.generate()
        while (isTokenExists(token).get() || mTokenHandler.isDeleted(token).get())
            mTokenGenerator.generate().also { token = it }
        return token
    }
}