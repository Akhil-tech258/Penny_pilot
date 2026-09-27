package com.privacyexpense.tracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.privacyexpense.tracker.data.db.AppDatabaseHelper

class TransactionNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Skip notifications from our own app
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        val result = TransactionDetectionEngine.parseNotification(sbn.packageName, title, text)
        if (result.isTransaction && result.transaction != null) {
            val dbHelper = AppDatabaseHelper.getInstance(applicationContext)

            // Prevent duplicate logs within 60s
            if (!dbHelper.isDuplicateTransaction(result.transaction.amount, result.transaction.merchant)) {
                val newId = dbHelper.insertTransaction(result.transaction)
                val savedTransaction = result.transaction.copy(id = newId)

                // Show the interactive 1-tap categorization notification
                val categories = dbHelper.getCategories()
                NotificationHelper.showInteractiveTransactionNotification(
                    applicationContext,
                    savedTransaction,
                    categories
                )
            }
        }
    }
}
