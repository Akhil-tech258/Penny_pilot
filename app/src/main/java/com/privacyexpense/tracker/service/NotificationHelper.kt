package com.privacyexpense.tracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.privacyexpense.tracker.MainActivity
import com.privacyexpense.tracker.R
import com.privacyexpense.tracker.data.model.Category
import com.privacyexpense.tracker.data.model.Transaction
import com.privacyexpense.tracker.data.model.TransactionType

object NotificationHelper {

    const val CHANNEL_ID = "expense_alerts_channel"
    const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showInteractiveTransactionNotification(
        context: Context,
        transaction: Transaction,
        categories: List<Category>,
        isPotentialDuplicate: Boolean = false
    ) {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = (NOTIFICATION_ID_BASE + transaction.id).toInt()

        // Content intent: Opens app on click
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "TRANSACTIONS")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isCredit = transaction.type == TransactionType.CREDIT
        val typeTitle = if (isCredit) {
            "₹${String.format("%.2f", transaction.amount)} credited"
        } else {
            "₹${String.format("%.2f", transaction.amount)} debited"
        }
        val typeSubtitle = if (isPotentialDuplicate) {
            "${transaction.merchant} • Possible duplicate alert"
        } else if (isCredit) {
            "Received from ${transaction.merchant} • Select category:"
        } else {
            "Paid to ${transaction.merchant} • What was this for?"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(typeTitle)
            .setContentText(typeSubtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        // 'Already Categorized' Action (Duplicate dismissal)
        val alreadyCategorizedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ALREADY_CATEGORIZED
            putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.id)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val alreadyCategorizedPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 95,
            alreadyCategorizedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 'Fake Transaction' Action (Remove & permanently ignore spam/promo)
        val fakeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_FAKE
            putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.id)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MERCHANT, transaction.merchant)
        }
        val fakePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 96,
            fakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 'Others' with WhatsApp-style direct text reply input (RemoteInput)
        val remoteInput = androidx.core.app.RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
            .setLabel("Type reason/note...")
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REPLY_NOTE
            putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.id)
            putExtra(NotificationActionReceiver.EXTRA_AMOUNT, transaction.amount)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 98,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Others (Type)",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        if (isPotentialDuplicate) {
            // When message arrives a second time, prioritize "Already Categorized"
            builder.addAction(0, "🔁 Already Done", alreadyCategorizedPendingIntent)
            builder.addAction(replyAction)
            builder.addAction(0, "🚫 Fake", fakePendingIntent)
        } else {
            // Standard notification: Category, Inline Note, Fake Transaction, and Already Categorized
            val topCategory = categories.firstOrNull { it.isNotificationEnabled && it.isEnabled && it.name != "Others" }
            if (topCategory != null) {
                val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_CATEGORIZE
                    putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.id)
                    putExtra(NotificationActionReceiver.EXTRA_CATEGORY_ID, topCategory.id)
                    putExtra(NotificationActionReceiver.EXTRA_CATEGORY_NAME, topCategory.name)
                    putExtra(NotificationActionReceiver.EXTRA_AMOUNT, transaction.amount)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (notificationId * 10 + topCategory.id).toInt(),
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, topCategory.name, actionPendingIntent)
            }
            builder.addAction(replyAction)
            builder.addAction(0, "🚫 Fake", fakePendingIntent)
            builder.addAction(0, "Already Done?", alreadyCategorizedPendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun showConfirmationNotification(
        context: Context,
        notificationId: Int,
        amount: Double,
        categoryName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "TRANSACTIONS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("✓ ₹${String.format("%.2f", amount)} saved as $categoryName")
            .setContentText("Added to local encrypted database")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(4000)

        notificationManager.notify(notificationId, builder.build())
    }
}
