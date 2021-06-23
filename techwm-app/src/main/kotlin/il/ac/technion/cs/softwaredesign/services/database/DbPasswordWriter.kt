package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordWriter
import library.DbFactory
import library.SecureStorageStringWrapper
import library.interfaces.IDbReader
import library.interfaces.IDbWriter
import java.util.concurrent.CompletableFuture

class DbPasswordWriter @Inject constructor (databaseFactory: DbFactory) : IDbPasswordWriter {
    private val dbPasswordsWriter : IDbWriter by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToPassword) }

    /**
     * Set password: write username -> password in DB
     *
     * @param username
     * @param password
     * @return CompletableFuture of the action
     */
    override fun setPassword(username: String, password: String) : CompletableFuture<Unit> {
        return dbPasswordsWriter.write(username, password)
    }
}