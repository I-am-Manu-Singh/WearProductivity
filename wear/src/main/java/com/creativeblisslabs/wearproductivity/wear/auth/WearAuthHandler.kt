package com.creativeblisslabs.wearproductivity.wear.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
//import androidx.credentials.CredentialManager
//import androidx.credentials.GetCredentialRequest
//import androidx.credentials.exceptions.GetCredentialCancellationException
//import com.google.android.libraries.identity.googleid.GetGoogleIdOption

object WearAuthHandler {
//
//    suspend fun getCredentialWithFallback(
//        activity: Activity,
//        webClientId: String,
//        onSecondaryAuthentication: () -> Unit
//    ) {
//        val credentialManager = CredentialManager.create(activity)
//        val googleIdOption = GetGoogleIdOption.Builder()
//            .setFilterByAuthorizedAccounts(false)
//            .setServerClientId(webClientId)
//            .setAutoSelectEnabled(true)
//            .build()
//
//        val request = GetCredentialRequest.Builder()
//            .addCredentialOption(googleIdOption)
//            .build()
//
//        try {
//            credentialManager.getCredential(activity, request)
//        } catch (_: GetCredentialCancellationException) {
//            onSecondaryAuthentication()
//        } catch (_: Exception) {
//            onSecondaryAuthentication()
//        }
//    }
//
//    fun startDeviceAuthGrant(context: Context, verificationUri: String) {
//        try {
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                addCategory(Intent.CATEGORY_BROWSABLE)
//                data = Uri.parse(verificationUri)
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            }
//            context.startActivity(intent)
//        } catch (_: Exception) {}
//    }
}
