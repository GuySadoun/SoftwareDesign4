package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.TokenWasDeletedAndCantBeReusedException
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbTokenHandler
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbTokenHandler @Inject constructor(databaseFactory: StorageFactoryImpl) : IDbTokenHandler {
    companion object {
        const val deletedTokenDbValue = ""
        const val usernamePrefix = "u"
    }

    private val dbTokenToUsernameHandler by lazy { databaseFactory.open(DbDirectoriesPaths.TokenToUsername, SerializerImpl()) }
    private val dbUsernameToTokenHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToToken, SerializerImpl()) }
    private val dbDeletedTokenHandler by lazy { databaseFactory.open(DbDirectoriesPaths.DeletedTokens, SerializerImpl()) }

    /**
     * Get username by token if token exists
     *
     * @param token
     * @return CompletableFuture of the username if token exists in DB, else CompletableFuture<null>
     */
    override fun getUsernameByToken(token: String): CompletableFuture<String?> {
        return isDeleted(token).thenCompose { deleted ->
            if (deleted) {
                CompletableFuture.completedFuture(null)
            } else {
                dbTokenToUsernameHandler.thenCompose {
                    it.read(token).thenApply { username -> username?.drop(usernamePrefix.length) }
                }
            }
        }
    }

    /**
     * Set or replace token to username
     * if token is deleted exception is thrown. else if username have previous token it have been deleted and writes the
     * new token we got as param bidirectional (token -> username and username -> token)
     *
     * @param token
     * @param username
     * @return CompletableFuture of the action
     */
    override fun setOrReplaceTokenToUsername(token: String, username: String) : CompletableFuture<Unit> {
        return isDeleted(token).thenCompose { deleted ->
            if (deleted)
                throw TokenWasDeletedAndCantBeReusedException()

            deleteUserPreviousTokenIfExist(username)
                .thenCompose { dbTokenToUsernameHandler.thenCompose { it.write(token, usernamePrefix + username) } }
                .thenCompose { dbUsernameToTokenHandler.thenCompose { it.write(username, token) } }
        }
    }

    /**
     * returns if token is deleted
     *
     * @param token
     * @return CompletableFuture of the answer
     */
    override fun isDeleted(token: String): CompletableFuture<Boolean> {
        return dbDeletedTokenHandler.thenCompose {
            it.read(token).thenApply { deletedIfNotNull ->
                deletedIfNotNull != null
            }
        }
    }

    /**
     * Delete user previous token if exist
     *
     * @param username
     * @return CompletableFuture of the action
     */
    override fun deleteUserPreviousTokenIfExist(username: String): CompletableFuture<Unit> {
        return getTokenByUsername(username).thenCompose { userPreviousToken ->
            if (userPreviousToken != null)
                deleteToken((userPreviousToken))
            else
                CompletableFuture.completedFuture(null)
        }
    }

    private fun deleteToken(token: String): CompletableFuture<Unit> {
        return dbDeletedTokenHandler.thenCompose { it.write(token, deletedTokenDbValue) }
    }

    private fun getTokenByUsername(username: String): CompletableFuture<String?> {
        return dbUsernameToTokenHandler.thenCompose { it.read(username) }
    }
}
