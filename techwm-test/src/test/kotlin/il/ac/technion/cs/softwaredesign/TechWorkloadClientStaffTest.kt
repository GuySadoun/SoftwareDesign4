package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.execution.CPUResource
import il.ac.technion.cs.softwaredesign.execution.GPUResource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class TechWorkloadClientStaffTest {
    private val injector = Guice.createInjector(TechWorkloadClientModule())
    private val clientFactory = injector.getInstance<TechWorkloadClientFactory>()
    private val manager = injector.getInstance<TechWorkloadManager>()

    @Test
    fun `A user can send a message to another user`() {
        manager.register("", "admin", "123456", PermissionLevel.USER)
            .thenCompose { manager.authenticate("admin", "123456") }
            .thenCompose { adminToken ->
                manager.register(adminToken, "john_smith", "654321", PermissionLevel.USER)
            }
            .join()

        val msg = "Hello, Admin!"

        val john = clientFactory.get("john_smith")
        val admin = clientFactory.get("admin")
        john.login("654321")
            .thenCompose { admin.login("123456") }
            .thenCompose { john.sendMessage("admin", msg) }
            .thenCompose { admin.inbox() }
            .thenAccept { inbox ->
                assertThat(inbox.size, equalTo(1))
                assertThat(inbox["john_smith"]!![0].message, equalTo(msg))
            }
            .join()
    }

    @Test
    fun `An admin approves a new users request`() {
        manager.register("", "admin", "123456", PermissionLevel.USER).join()

        val john = clientFactory.get("john_smith")
        val admin = clientFactory.get("admin") as TechWorkloadAdminClient
        val rsn = "Pretty please?"

        john.requestAccessToSystem("john_smith", "pass", rsn)
            .thenCompose { admin.login("123456") }
            .thenCompose { admin.accessRequests() }
            .thenCompose { requests ->
                assertThat(requests.size, equalTo(1))
                assertThat(requests.first().reason, equalTo(rsn))
                requests.first().approve()
            }
            .thenCompose {
                assertDoesNotThrow { john.login("pass") }
            }
            .join()
    }
}