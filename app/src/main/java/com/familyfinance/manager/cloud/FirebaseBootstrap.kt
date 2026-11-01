package com.familyfinance.manager.cloud

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseBootstrap {

    fun initialize(context: Context): Boolean = try {
        FirebaseApp.getApps(context).isNotEmpty() ||
                FirebaseApp.initializeApp(context) != null
    } catch (_: Exception) {
        false
    }
}
