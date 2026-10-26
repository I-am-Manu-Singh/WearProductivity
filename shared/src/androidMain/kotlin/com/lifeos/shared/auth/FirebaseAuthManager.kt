package com.creativeblisslabs.wearproductivity.shared.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class LifeAuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

object FirebaseAuthManager {

    val currentUser: LifeAuthUser?
        get() {
            val user: FirebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
            return LifeAuthUser(
                uid = user.uid,
                email = user.email ?: "user@gmail.com",
                displayName = user.displayName ?: "LifeOS User",
                photoUrl = user.photoUrl?.toString()
            )
        }

    fun isUserSignedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null

    suspend fun signInWithGoogle(context: Context, webClientId: String): Result<LifeAuthUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val idToken = result.credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")

            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    Result.success(
                        LifeAuthUser(
                            uid = user.uid,
                            email = user.email,
                            displayName = user.displayName,
                            photoUrl = user.photoUrl?.toString()
                        )
                    )
                } else {
                    Result.failure(Exception("Firebase user is null"))
                }
            } else {
                Result.failure(Exception("Google ID token not found in credential"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
    }
}
