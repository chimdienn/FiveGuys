package com.example.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "biomate_session")

/**
 * Persists which local account is signed in, so the session survives a process restart
 * (spec section 7: "persistent authentication session").
 *
 * Stores only a uid and an email — never a credential, never a token.
 */
class SessionStore(private val context: Context) {

    private val uidKey = stringPreferencesKey("uid")
    private val emailKey = stringPreferencesKey("email")

    val session: Flow<StoredSession?> = context.sessionDataStore.data.map { prefs ->
        val uid = prefs[uidKey]
        val email = prefs[emailKey]
        if (uid.isNullOrBlank() || email.isNullOrBlank()) null else StoredSession(uid, email)
    }

    suspend fun save(uid: String, email: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[uidKey] = uid
            prefs[emailKey] = email
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}

data class StoredSession(val uid: String, val email: String)
