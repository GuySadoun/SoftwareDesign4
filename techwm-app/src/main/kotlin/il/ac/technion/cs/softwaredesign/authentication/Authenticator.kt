package il.ac.technion.cs.softwaredesign.authentication

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.TokenDoesNotExistException
import il.ac.technion.cs.softwaredesign.services.JobManager
import il.ac.technion.cs.softwaredesign.services.interfaces.resource.IResourceManager
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserPasswordVerifier
import java.util.concurrent.CompletableFuture

class Authenticator @Inject constructor(private val mTokenManager: ITokenManager,
                                        private val mUserPasswordVerifier: IUserPasswordVerifier,
                                        private val mUserManager : IUserManager,
                                        private val mResourceManager: IResourceManager,
                                        private val mJobManager: JobManager
) {
    /**
     * Authenticate user by username and password
     *
     * @param username
     * @param password
     * @return authenticatedUser object of the user
     */
    fun authenticate(username: String, password: String): CompletableFuture<AuthenticatedUser> {
        return mUserManager.isUserRevoked(username)
            .thenCombine(mUserPasswordVerifier.isUsernamePasswordMatch(username, password)) {isRevoked, passwordMatch ->
                if (isRevoked || !passwordMatch )
                    throw IllegalArgumentException()
        }.thenCompose {
            mTokenManager.createOrReplaceUserToken(username).thenApply { token ->
                AuthenticatedUser(token, username, mUserManager, mTokenManager, mResourceManager, mJobManager)
            }
        }
    }

    /**
     * Authenticate user with a token
     *
     * @param token
     * @return authenticatedUser object of the user
     */
    fun authenticate(token: String): CompletableFuture<AuthenticatedUser> {
        return mTokenManager.getUsernameByTokenIfExists(token).thenApply { username ->
            if (username == null)
                throw TokenDoesNotExistException()
            AuthenticatedUser(token, username, mUserManager, mTokenManager, mResourceManager, mJobManager)
        }
    }
}