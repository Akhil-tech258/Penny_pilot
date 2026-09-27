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
        categories: List<Category>
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
        val typeSubtitle = if (isCredit) {
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

        // Add top 2-3 category action buttons
        val eligibleCategories = categories.filter { it.isNotificationEnabled && it.isEnabled && it.name != "Others" }.take(2)
        for (category in eligibleCategories) {
            val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CATEGORIZE
                putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.id)
                putExtra(NotificationActionReceiver.EXTRA_CATEGORY_ID, category.id)
                putExtra(NotificationActionReceiver.EXTRA_CATEGORY_NAME, category.name)
                putExtra(NotificationActionReceiver.EXTRA_AMOUNT, transaction.amount)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                (notificationId * 10 + category.id).toInt(),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, category.name, actionPendingIntent)
        }

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
        builder.addAction(replyAction)

        // 'More...' action button to view in-app
        val moreIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "TRANSACTIONS")
            putExtra("EXTRA_TRANSACTION_ID", transaction.id)
        }
        val morePendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 99,
            moreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "More...", morePendingIntent)

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

    fun showDailyFlightLog(
        context: Context,
        spentToday: Double,
        receivedToday: Double,
        transactionCount: Int
    ) {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "TRANSACTIONS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✈️ Flight Log: ₹${String.format("%.2f", spentToday)} spent today")
            .setContentText("$transactionCount transaction(s) logged. Received: ₹${String.format("%.2f", receivedToday)}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Today's financial velocity: ₹${String.format("%.2f", spentToday)} outflow across $transactionCount transactions. Offline & encrypted."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(9999, builder.build())
    }
}
