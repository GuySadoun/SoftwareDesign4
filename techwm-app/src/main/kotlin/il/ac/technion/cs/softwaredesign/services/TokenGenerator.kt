package il.ac.technion.cs.softwaredesign.services

import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenGenerator
import kotlin.random.Random

/**
 * Token generator of the system
 *
 * @constructor Create empty Token generator
 */
class TokenGenerator: ITokenGenerator {
    companion object {
        internal const val tokenLen = 16
        internal val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    }

    /**
     * Generate a new token
     *
     * @return
     */
    override fun generate(): String {
        return (1..tokenLen)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}
