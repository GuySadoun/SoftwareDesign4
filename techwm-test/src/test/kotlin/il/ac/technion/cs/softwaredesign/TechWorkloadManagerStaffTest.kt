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
import java.util.EnumSet.range
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class TechWorkloadManagerStaffTest {
    private val injector = Guice.createInjector(TechWorkloadManagerModule())
    private val manager = injector.getInstance<TechWorkloadManager>()

    private fun registerFirstUser(): CompletableFuture<Pair<String, String>> {
        val username = "admin"
        val password = "123456"

        return manager.register("", username, password, PermissionLevel.USER)
            .thenApply { username to password }
    }

    @Test
    fun `a non-existing user throws exception on authenticate`() {
        val username = "non-existing"
        val password = "non-existing"

        val throwable = assertThrows<CompletionException> {
            manager.authenticate(username, password).join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }
/*
    @Test
    fun stressTest() {
        val username = "admin"
        val password = "123456"

        val something = manager.register("", username, password, PermissionLevel.USER)
            .thenApply { username to password }.join()

        val adminToken = manager.authenticate(username, password).join()
        for(i in 0..200)
            manager.attachHardwareResource(adminToken, "$i", "name$i").join()

        for (i in 1..50)
            manager.submitJob(adminToken, "job1", listOf())
        manager.submitJob(adminToken, "job1", listOf()).join()
    }
*/
    @Test
    fun ourTest(){
        val username = "admin"
        val password = "123456"

        manager.register("", username, password, PermissionLevel.USER).join()
        val adminToken = manager.authenticate(username, password).join()
        manager.register(adminToken, "ss", "ss", PermissionLevel.USER).join()
        val userToken = manager.authenticate("ss", "ss").join()

        for(i in 0..10)
            manager.attachHardwareResource(adminToken, "c$i", "name$i").join()

        val actual1 = manager.submitJob(userToken, "job1", listOf("c1")).join()
        val actual2 = manager.submitJob(adminToken, "job1", listOf("c1"))
        Thread.sleep(300)
        val actual3 = manager.submitJob(adminToken, "job1", listOf("c1"))
        Thread.sleep(300)

        println(manager.jobInformation(adminToken, "0").join())
        println(manager.jobInformation(adminToken, "1").join())
        println(manager.jobInformation(adminToken, "2").join())
        println("--------------------------------------------------")
        manager.cancelJob(userToken, "0").join()
        actual2.join()

        println(manager.jobInformation(adminToken, "0").join())
        println(manager.jobInformation(adminToken, "1").join())
        println(manager.jobInformation(adminToken, "2").join())
        println("--------------------------------------------------")
        actual2.join().finishJob().join()

        println(manager.jobInformation(adminToken, "0").join())
        println(manager.jobInformation(adminToken, "1").join())
        println(manager.jobInformation(adminToken, "2").join())

        actual3.join()
        actual3.join().finishJob().join()

        println(manager.jobInformation(adminToken, "0").join())
        println(manager.jobInformation(adminToken, "1").join())
        println(manager.jobInformation(adminToken, "2").join())
    }
    @Test
    fun `not exist resource`(){
        val username = "admin"
        val password = "123456"

        manager.register("", username, password, PermissionLevel.USER).join()
        val adminToken = manager.authenticate(username, password).join()
        manager.register(adminToken, "ss", "ss", PermissionLevel.USER).join()
        val userToken = manager.authenticate("ss", "ss").join()
        manager.changeAccountType(adminToken, "ss", AccountType.ROOT).join()

        for(i in 0..10)
            manager.attachHardwareResource(adminToken, "$i", "name$i").join()

        val throwable = assertThrows<CompletionException> {
            // A [PermissionException] is thrown because there already exists a user in the system and the
            // token is invalid
            manager.submitJob(userToken, "job1", listOf("11412")).join()
        }
        assertThat(throwable.cause!!, isA<java.lang.IllegalArgumentException>())
    }

    @Test
    fun `first admin user is successfully registered`() {
        val username = "admin"
        val password = "123456"

        assertDoesNotThrow {
            manager.register("", username, password, PermissionLevel.USER).join()
        }
        val throwable = assertThrows<CompletionException> {
            // A [PermissionException] is thrown because there already exists a user in the system and the
            // token is invalid
            manager.register("", username, password, PermissionLevel.USER).join()
        }
        assertThat(throwable.cause!!, isA<PermissionException>())
    }

    @Test
    fun `admin user has the correct permission level`() {
        val (username, password) = registerFirstUser().join()

        assertThat(
            manager.authenticate(username, password)
                .thenCompose { manager.userInformation(it, username) }
                .thenApply { it!!.permissionLevel }
                .join(),
            equalTo(PermissionLevel.ADMINISTRATOR))
    }

    @Test
    fun `admin cannot change his own permissions`() {
        val (username, password) = registerFirstUser().join()
        val throwable = assertThrows<CompletionException> {
            manager.authenticate(username, password)
                .thenCompose { token -> manager.changePermissions(token, username, PermissionLevel.USER) }
                .join()
        }

        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `user cannot change account type of an operator`() {
        val (adminUsername, password) = registerFirstUser().join()
        val username = "user"

        val throwable = assertThrows<CompletionException> {
            manager.authenticate(adminUsername, password)
                .thenCompose { token ->
                    manager.register(token, username, "654321", PermissionLevel.USER)
                        .thenCompose { manager.changePermissions(token, username, PermissionLevel.OPERATOR) }
                        .thenCompose { manager.changeAccountType(token, username, AccountType.RESEARCH) }
                }.join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `a single job is allocated when there are enough resources`() {
        val (adminUsername, password) = registerFirstUser().join()

        manager.authenticate(adminUsername, password)
            .thenCompose { token ->
                manager.attachHardwareResource(token, "cpu-1", "Intel Xeon 8 Core")
                    .thenCompose { manager.attachHardwareResource(token, "gpu-1", "NVIDIA RTX 2080 Ti") }
                    .thenCompose { manager.submitJob(token, "first-job", listOf("cpu-1", "gpu-1")) }
                    .thenCompose { jobAllocation -> jobAllocation.resources().thenApply { Pair(jobAllocation, it) } }
                    .thenCompose { (allocation, resources) ->
                        assertThat(resources[0], isA<CPUResource>())
                        assertThat(resources[1], isA<GPUResource>())
                        allocation.finishJob()
                    }
            }.join()
    }

    @Test
    fun `A default account type cannot request more resources than allowed`() {
        val (adminUsername, password) = registerFirstUser().join()

        val throwable = assertThrows<CompletionException> {
            manager.authenticate(adminUsername, password)
                .thenCompose { adminToken ->
                    manager.attachHardwareResource(adminToken, "cpu-1", "Intel Xeon 8 Core")
                        .thenCompose { manager.attachHardwareResource(adminToken, "gpu-1", "NVIDIA RTX 2080 Ti") }
                        .thenCompose { manager.register(adminToken, "user", "password", PermissionLevel.USER) }
                }.thenCompose {
                    manager.authenticate("user", "password")
                        .thenCompose { token -> manager.submitJob(token, "first-job", listOf("cpu-1", "gpu-1")) }
                        .thenCompose(AllocatedJob::finishJob)
                }.join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }
}