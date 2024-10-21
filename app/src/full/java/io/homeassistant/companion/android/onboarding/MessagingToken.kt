package io.homeassistant.companion.android.onboarding

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

suspend fun getFirebaseMessagingToken(): String {
    return try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: Exception) {
        Log.e("FirebaseMessagingToken", "Issue getting token", e)
        ""
    }
}
