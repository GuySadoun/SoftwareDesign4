package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordWriter
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbPasswordWriter @Inject constructor (databaseFactory: StorageFactoryImpl) : IDbPasswordWriter {
    private val dbPasswordsWriter by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToPassword, SerializerImpl()) }

    /**
     * Set password: write username -> password in DB
     *
     * @param username
     * @param password
     * @return CompletableFuture of the action
     */
    override fun setPassword(username: String, password: String) : CompletableFuture<Unit> {
        return dbPasswordsWriter.thenCompose {
            it.write(username, password)
        }
    }
}