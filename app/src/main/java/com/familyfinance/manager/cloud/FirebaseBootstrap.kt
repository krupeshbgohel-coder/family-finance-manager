package com.familyfinance.manager.cloud

import android.content.Context
import com.familyfinance.manager.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/** Firebase is optional until the owner adds their own Firebase web/app configuration in res/values/firebase.xml. */
object FirebaseBootstrap {
    fun initialize(context: Context): FirebaseApp? {
        FirebaseApp.getApps(context).firstOrNull()?.let { return it }
        val apiKey = context.getString(R.string.firebase_api_key).trim()
        val appId = context.getString(R.string.firebase_app_id).trim()
        val projectId = context.getString(R.string.firebase_project_id).trim()
        val senderId = context.getString(R.string.firebase_messaging_sender_id).trim()
        if (apiKey.isBlank() || appId.isBlank() || projectId.isBlank() || senderId.isBlank() || apiKey.startsWith("PASTE_")) return null
        return runCatching {
            FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setApiKey(apiKey).setApplicationId(appId).setProjectId(projectId)
                .setGcmSenderId(senderId).build())
        }.getOrNull()
    }
}
