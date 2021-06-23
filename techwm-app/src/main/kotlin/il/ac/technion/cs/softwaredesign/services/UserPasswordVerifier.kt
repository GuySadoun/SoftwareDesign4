package il.ac.technion.cs.softwaredesign.services

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.database.DbPasswordReader
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordReader
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserPasswordVerifier
import library.DbFactory
import java.util.concurrent.CompletableFuture

class UserPasswordVerifier @Inject constructor (private val passwordsReader: IDbPasswordReader): IUserPasswordVerifier {

    /**
     * return whether the username and the password match
     *
     * @param username
     * @param password
     * @return CompletableFuture of the result
     */
    override fun isUsernamePasswordMatch(username: String, password: String): CompletableFuture<Boolean> {
        return passwordsReader.getPassword(username).thenApply {
            it == password
        }
    }
}