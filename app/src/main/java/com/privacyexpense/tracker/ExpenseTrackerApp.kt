package com.privacyexpense.tracker

import android.app.Application
import com.privacyexpense.tracker.service.NotificationHelper

class ExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channel for interactive expense categorization
        NotificationHelper.createNotificationChannel(this)
    }
}
