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

        val fullContent = "${title ?: ""} ${text ?: ""}".trim()
        val dbHelper = AppDatabaseHelper.getInstance(applicationContext)

        // Skip if message matches any pattern user previously marked as fake
        if (dbHelper.isFakePattern(fullContent)) {
            return
        }

        val result = TransactionDetectionEngine.parseNotification(sbn.packageName, title, text)
        if (result.isTransaction && result.transaction != null) {
            // Check if this merchant/sender was previously marked as fake
            if (dbHelper.isFakePattern(result.transaction.merchant)) {
                return
            }

            // Check if user already has a similar transaction logged recently
            val isPotentialDuplicate = dbHelper.hasRecentSimilarTransaction(result.transaction.amount)

            // Prevent duplicate logs across channels (UPI + Bank SMS within 180s)
            if (!dbHelper.isDuplicateOrEnrich(result.transaction)) {
                val newId = dbHelper.insertTransaction(result.transaction)
                val savedTransaction = result.transaction.copy(id = newId)

                // Show the interactive 1-tap categorization notification
                val categories = dbHelper.getCategories()
                NotificationHelper.showInteractiveTransactionNotification(
                    applicationContext,
                    savedTransaction,
                    categories,
                    isPotentialDuplicate = isPotentialDuplicate
                )
            }
        }
    }
}
