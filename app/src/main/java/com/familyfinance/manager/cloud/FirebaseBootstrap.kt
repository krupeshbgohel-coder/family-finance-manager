package com.familyfinance.manager.cloud

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseBootstrap {

    fun initialize(context: Context): FirebaseApp? {
        return try {
            val existingApps = FirebaseApp.getApps(context)

            if (existingApps.isNotEmpty()) {
                existingApps.first()
            } else {
                FirebaseApp.initializeApp(context)
            }
        } catch (_: Exception) {
            null
        }
    }
}
