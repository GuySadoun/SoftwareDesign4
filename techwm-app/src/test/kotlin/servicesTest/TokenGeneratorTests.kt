package servicesTest

import il.ac.technion.cs.softwaredesign.services.TokenGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TokenGeneratorTests {
    @Test
    fun `generate token of the required length`() {
        // Arrange
        val tokenGenerator = TokenGenerator()

        // Act
        val generatedToken = tokenGenerator.generate()

        // Assert
        Assertions.assertEquals(TokenGenerator.tokenLen, generatedToken.length)
    }

    @Test
    fun `generator does not generate the same token every time`() {
        // Arrange
        val testTolerance = 10
        val tokenGenerator = TokenGenerator()
        val firstGeneratedToken = tokenGenerator.generate()

        // Act & Assert
        for (i in 1..testTolerance) {
            val generated = tokenGenerator.generate()
            if (generated != firstGeneratedToken)
                return
        }
    }
}