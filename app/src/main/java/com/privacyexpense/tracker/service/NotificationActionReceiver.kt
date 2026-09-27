package com.privacyexpense.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privacyexpense.tracker.data.db.AppDatabaseHelper

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CATEGORIZE = "com.privacyexpense.tracker.ACTION_CATEGORIZE_TRANSACTION"
        const val ACTION_REPLY_NOTE = "com.privacyexpense.tracker.ACTION_REPLY_NOTE"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        const val EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID"
        const val EXTRA_CATEGORY_NAME = "EXTRA_CATEGORY_NAME"
        const val EXTRA_AMOUNT = "EXTRA_AMOUNT"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val dbHelper = AppDatabaseHelper.getInstance(context)

        if (intent.action == ACTION_REPLY_NOTE) {
            val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
            val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

            val remoteInputResults = androidx.core.app.RemoteInput.getResultsFromIntent(intent)
            val customText = remoteInputResults?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim() ?: "Others"

            if (transactionId > 0) {
                dbHelper.updateTransactionCategory(
                    transactionId = transactionId,
                    categoryId = null,
                    categoryName = "Others",
                    description = customText
                )
                NotificationHelper.showConfirmationNotification(context, notificationId, amount, customText)
            }
        } else if (intent.action == ACTION_CATEGORIZE) {
            val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
            val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
            val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Categorized"
            val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

            if (transactionId > 0 && categoryId > 0) {
                dbHelper.updateTransactionCategory(transactionId, categoryId, categoryName)
                NotificationHelper.showConfirmationNotification(context, notificationId, amount, categoryName)
            }
        }
    }
}
