package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.AccountType
import il.ac.technion.cs.softwaredesign.AccountType.*
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.PermissionLevel.*
import il.ac.technion.cs.softwaredesign.User
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbUserInfoHandler
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbUserInfoHandler @Inject constructor(databaseFactory: StorageFactoryImpl) : IDbUserInfoHandler {
    companion object {
        const val usernameSuffix = "_username"
        const val accountSuffix = "_account"
        const val permissionSuffix = "_permission"
        const val loginSuffix = "_login"
    }

    private val dbUsernameToUserHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToUser, SerializerImpl()) }
    private val dbIsUsernameRevokedHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameIsRevoked, SerializerImpl()) }

    private val accountTypeArray = mapOf<Int?, AccountType>(
        DEFAULT.ordinal to DEFAULT,
        RESEARCH.ordinal to RESEARCH,
        ROOT.ordinal to ROOT
    )

    private val permissionsLevelArray = mapOf<Int?, PermissionLevel>(
                USER.ordinal to USER,
                OPERATOR.ordinal to OPERATOR,
                ADMINISTRATOR.ordinal to ADMINISTRATOR
            )

    /**
     * Get user by username if username exists in DB
     *
     * @param username
     * @return CompletableFuture of the user if username exists in DB, else CompletableFuture<null>
     */
    override fun getUserByUsername(username: String): CompletableFuture<User?> {
        return dbUsernameToUserHandler.thenCompose { usernameToUserStorage ->
            usernameToUserStorage.read(username + usernameSuffix).thenCompose { usernameString ->
                usernameToUserStorage.read(username + accountSuffix).thenApply { userAccountType ->
                    Pair(usernameString, accountTypeArray[userAccountType?.toInt()])
                }
            }.thenCompose { usernameAccountTypePair ->
                usernameToUserStorage.read(username + permissionSuffix).thenApply { userPermissionLevel ->
                    Triple(
                        usernameAccountTypePair.first,
                        usernameAccountTypePair.second,
                        permissionsLevelArray[userPermissionLevel?.toInt()]
                    )
                }
            }
            .thenApply { userDetailsTriple ->
                val usernameString = userDetailsTriple.first
                val accountType = userDetailsTriple.second
                val permissionLevel = userDetailsTriple.third

                if (usernameString != null && accountType != null && permissionLevel != null)
                    User(usernameString, accountType, permissionLevel)
                else
                    null
            }
        }
    }

    /**
     * writes map of username to each one of User members in the DB
     *
     * @param user
     * @return CompletableFuture of the action
     */
    override fun setUsernameToUser(user: User): CompletableFuture<Unit> {
        val username = user.username
        val accountType = user.account.ordinal.toString()
        val permission = user.permissionLevel.ordinal.toString()
        return dbUsernameToUserHandler.thenCompose { usernameToUserStorage ->
            usernameToUserStorage.write(username + usernameSuffix, username)
                .thenCompose { usernameToUserStorage.write(username + accountSuffix, accountType) }
                .thenCompose { usernameToUserStorage.write(username + permissionSuffix, permission) }
        }
    }

    /**
     * Get user permission level by username
     *
     * @param username
     * @return CompletableFuture of PermissionLevel if username exists in DB, else CompletableFuture<null>
     */
    override fun getUserPermissionLevel(username: String): CompletableFuture<PermissionLevel?> {
        return dbUsernameToUserHandler.thenCompose { usernameToUserStorage ->
            usernameToUserStorage.read(username + permissionSuffix).thenApply { user ->
                permissionsLevelArray[user?.toInt()]
            }
        }
    }

    /**
     * return if username is revoked
     *
     * @param username
     * @return CompletableFuture of the answer
     */
    override fun isUserRevoked(username: String): CompletableFuture<Boolean> {
        return dbIsUsernameRevokedHandler.thenCompose { isUsernameRevokedStorage ->
            isUsernameRevokedStorage.read(username).thenApply {
                it == "1"
            }
        }
    }

    /**
     * Revoke user from the system:
     * writes his name in the revoked list in DB
     *
     * @param username
     * @return CompletableFuture of the action
     */
    override fun revokeUser(username: String): CompletableFuture<Unit> {
        return dbIsUsernameRevokedHandler.thenCompose { isUsernameRevokedStorage ->
            isUsernameRevokedStorage.write(username, "1")
        }
    }

    /**
     * Clear name from revoked list:
     * mark the revoked user as non-revoked in the revoked list (his name stays in the list!)
     *
     * @param username
     * @return CompletableFuture of the action
     */
    override fun clearNameFromRevokedList(username: String): CompletableFuture<Unit> {
        return dbIsUsernameRevokedHandler.thenCompose { isUsernameRevokedStorage ->
            isUsernameRevokedStorage.write(username, "0")
        }
    }

    override fun getUsernameState(username: String): CompletableFuture<Boolean> {
        return dbUsernameToUserHandler.thenCompose { usernameToUserStorage ->
            usernameToUserStorage.read(username + loginSuffix).thenApply { state ->
                state == "1"
            }
        }
    }

    override fun setUserLoginState(username: String, state: Boolean): CompletableFuture<Unit> {
        return dbUsernameToUserHandler.thenCompose { usernameToUserStorage ->
            val stateString = if(state) "1" else "0"
            usernameToUserStorage.write(username + loginSuffix, stateString)
        }
    }
}