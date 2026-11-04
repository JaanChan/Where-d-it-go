package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [36])
class MemoryRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveMemory_authenticatedOwner_succeedsAndTracksHistory() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MemoryRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveMemory(
                itemName = "School ID",
                location = "Second drawer",
                category = "ID & Cards",
                description = "Next to notebook",
                addedByName = "Alice"
            )
        }
        assertTrue("Expected saveMemory to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val memoryId = saveResult.getOrThrow()
        assertTrue(memoryId.isNotBlank())

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePrivateMemories(aliceUid).first { list -> list.any { it.id == memoryId } }
        }
        val created = emitted.first { it.id == memoryId }
        assertEquals("School ID", created.itemName)
        assertEquals("Second drawer", created.location)
    }

    @Test
    fun getPrivateMemory_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MemoryRepository(firestore)
        val memoryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveMemory(
                itemName = "Passport",
                location = "Locked safe",
                category = "Documents",
                description = "Top shelf",
                addedByName = "Alice"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = MemoryRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getPrivateMemoryById(aliceUid, memoryId)
        }
        assertTrue("Expected cross-user read to fail", crossResult.isFailure)
        val fsEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observePrivateMemories_unauthenticated_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = MemoryRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePrivateMemories(aliceUid).first()
            }
            fail("Expected unauthenticated query to throw")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun createSharedSpaceAndJoinByCode_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = MemoryRepository(firestore)
        val space = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createSharedSpace(
                name = "Family Home",
                ownerName = "Alice",
                initialInvitedEmail = BOB_EMAIL
            ).getOrThrow()
        }
        assertTrue(space.inviteCode.isNotBlank())

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = MemoryRepository(firestore)
        val joinedSpaceName = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinSharedSpaceByCode(space.inviteCode, "Bob").getOrThrow()
        }
        assertEquals("Family Home", joinedSpaceName)

        val sharedMemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.saveMemory(
                itemName = "TV Remote",
                location = "Living room coffee table",
                category = "Electronics",
                description = "Under magazine",
                spaceId = space.id,
                spaceMemberUids = space.memberUids + bobUid,
                addedByName = "Bob"
            ).getOrThrow()
        }
        assertTrue(sharedMemId.isNotBlank())
    }

    private companion object {
        const val ALICE_EMAIL = "alice_rule@test.com"
        const val BOB_EMAIL = "bob_rule@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
