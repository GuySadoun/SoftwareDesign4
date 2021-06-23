package il.ac.technion.cs.softwaredesign

import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.authentication.Authenticator
import il.ac.technion.cs.softwaredesign.execution.ExecutionServiceModule
import il.ac.technion.cs.softwaredesign.services.*
import il.ac.technion.cs.softwaredesign.services.database.DbPasswordReader
import il.ac.technion.cs.softwaredesign.services.database.DbPasswordWriter
import il.ac.technion.cs.softwaredesign.services.database.DbTokenHandler
import il.ac.technion.cs.softwaredesign.services.database.DbUserInfoHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordReader
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordWriter
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbTokenHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbUserInfoHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.resource.IResourceManager
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenGenerator
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserPasswordVerifier
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import main.kotlin.StorageFactoryImpl

class TechWorkloadManagerModule : KotlinModule() {
    override fun configure() {
        bind<Authenticator>().`in`<Singleton>()

        bind<AdminCreator>().`in`<Singleton>()
        bind<ITokenGenerator>().to<TokenGenerator>().`in`<Singleton>()
        bind<ITokenManager>().to<TokenManager>().`in`<Singleton>()
        bind<IUserManager>().to<UserManager>().`in`<Singleton>()
        bind<IResourceManager>().to<ResourceManager>().`in`<Singleton>()
        bind<JobManager>().`in`<Singleton>()
        bind<IUserPasswordVerifier>().to<UserPasswordVerifier>().`in`<Singleton>()

        bind<IDbPasswordReader>().to<DbPasswordReader>().`in`<Singleton>()
        bind<IDbPasswordWriter>().to<DbPasswordWriter>().`in`<Singleton>()
        bind<IDbTokenHandler>().to<DbTokenHandler>().`in`<Singleton>()
        bind<IDbUserInfoHandler>().to<DbUserInfoHandler>().`in`<Singleton>()

        bind<StorageFactoryImpl>().`in`<Singleton>()

        install(SecureStorageModule())
        install(ExecutionServiceModule())
    }
}