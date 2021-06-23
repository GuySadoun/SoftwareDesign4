package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordReader
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbPasswordReader @Inject constructor (databaseFactory: StorageFactoryImpl) : IDbPasswordReader {
    private val dbPasswordsReader by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToPassword, SerializerImpl()) }

    /**
     * Get password from DB by username
     *
     * @param username
     * @return CompletableFuture of the password if username exists, else CompletableFuture<null>
     */
    override fun getPassword(username: String): CompletableFuture<String?> {
        return dbPasswordsReader.thenCompose {
            it.read(username)
        }
    }
}