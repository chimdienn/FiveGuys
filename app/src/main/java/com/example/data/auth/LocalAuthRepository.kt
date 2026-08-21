package com.example.data.auth

import com.example.data.local.BiomateDaoV2
import com.example.data.local.LocalCredentialEntity
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Authentication backed by the on-device database.
 *
 * Used when no Firebase project is configured, so the app is runnable and demonstrable
 * out of the box. It is a genuine credential store — salted PBKDF2 hashes, no recoverable
 * passwords — but it is single-device by nature: two accounts can interact within one
 * installation, not across installations. [com.example.data.remote.FirebaseAuthRepository]
 * is the multi-device path.
 */
class LocalAuthRepository(
    private val dao: BiomateDaoV2,
    private val sessionStore: SessionStore,
    /**
     * Suspends until first-run seeding has finished.
     *
     * Without this, a sign-in during the seeding window would look up a credential table
     * that is still being written and report a wrong password for a correct one.
     */
    private val awaitReady: suspend () -> Unit = {},
    private val nowMillis: () -> Long = System::currentTimeMillis
) : AuthRepository {

    @Volatile
    private var cachedUid: String? = null

    override val authState: Flow<AuthState> = sessionStore.session.map { stored ->
        cachedUid = stored?.uid
        if (stored == null) AuthState.SignedOut else AuthState.SignedIn(stored.uid, stored.email)
    }

    override val currentUid: String? get() = cachedUid

    override suspend fun signIn(email: String, password: String): Result<String> {
        val normalised = email.trim().lowercase()
        if (normalised.isEmpty() || password.isEmpty()) {
            return Result.failure(AuthException("Enter your email and password."))
        }
        awaitReady()

        val credential = dao.getCredential(normalised)
            // Deliberately the same message as a wrong password: revealing which emails
            // are registered would turn the login form into an account enumeration oracle.
            ?: return Result.failure(AuthException("Incorrect email or password."))

        val valid = PasswordHasher.verify(
            password = password,
            saltHex = credential.salt,
            expectedHash = credential.passwordHash,
            iterations = credential.iterations
        )
        if (!valid) return Result.failure(AuthException("Incorrect email or password."))

        sessionStore.save(credential.uid, normalised)
        cachedUid = credential.uid
        return Result.success(credential.uid)
    }

    override suspend fun register(email: String, password: String, displayName: String): Result<String> {
        val normalised = email.trim().lowercase()
        validateEmail(normalised)?.let { return Result.failure(AuthException(it)) }
        validatePassword(password)?.let { return Result.failure(AuthException(it)) }
        if (displayName.isBlank()) {
            return Result.failure(AuthException("Enter a display name."))
        }
        // Also gated: registering while the seeder is mid-write could otherwise create an
        // account with an email the seeder is about to claim.
        awaitReady()
        if (dao.getCredential(normalised) != null) {
            return Result.failure(AuthException("An account with that email already exists."))
        }

        val uid = "local_" + UUID.randomUUID().toString().replace("-", "").take(20)
        val salt = PasswordHasher.newSalt()
        dao.upsertCredential(
            LocalCredentialEntity(
                email = normalised,
                uid = uid,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                iterations = PasswordHasher.DEFAULT_ITERATIONS,
                createdAt = nowMillis()
            )
        )
        sessionStore.save(uid, normalised)
        cachedUid = uid
        return Result.success(uid)
    }

    /**
     * Local builds have no email transport, so there is nothing honest to do here.
     *
     * Returning a failure with a clear explanation is better than pretending an email was
     * sent and leaving the user waiting for it.
     */
    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.failure(
        AuthException(
            "Password reset needs a Firebase project. This build is running on the local " +
                "backend, which cannot send email."
        )
    )

    override suspend fun signOut() {
        cachedUid = null
        sessionStore.clear()
    }

    companion object {
        fun validateEmail(email: String): String? = when {
            email.isBlank() -> "Enter your email address."
            !email.contains('@') || !email.substringAfter('@').contains('.') ->
                "That does not look like an email address."
            else -> null
        }

        fun validatePassword(password: String): String? = when {
            password.length < 8 -> "Use at least 8 characters."
            password.none { it.isDigit() } -> "Include at least one number."
            password.none { it.isLetter() } -> "Include at least one letter."
            else -> null
        }
    }
}

class AuthException(message: String) : Exception(message)
