package com.privacyexpense.tracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.privacyexpense.tracker.data.db.AppDatabaseHelper
import com.privacyexpense.tracker.data.model.Category
import com.privacyexpense.tracker.data.model.LocalProfile
import com.privacyexpense.tracker.data.model.Transaction
import com.privacyexpense.tracker.data.model.TransactionStatus
import com.privacyexpense.tracker.data.model.TransactionType
import com.privacyexpense.tracker.data.security.CryptoManager
import com.privacyexpense.tracker.service.NotificationHelper
import com.privacyexpense.tracker.service.TransactionDetectionEngine
import com.privacyexpense.tracker.ui.screens.BackupScreen
import com.privacyexpense.tracker.ui.screens.CreateProfileScreen
import com.privacyexpense.tracker.ui.screens.CustomTransactionsScreen
import com.privacyexpense.tracker.ui.screens.LoginScreen
import com.privacyexpense.tracker.ui.screens.OpeningSplashScreen
import com.privacyexpense.tracker.ui.screens.SettingsScreen
import com.privacyexpense.tracker.ui.screens.SignatureSplitHomeScreen
import com.privacyexpense.tracker.data.model.RecurringBill
import com.privacyexpense.tracker.ui.screens.RecurringBillsScreen
import com.privacyexpense.tracker.ui.screens.WelcomeScreen
import com.privacyexpense.tracker.ui.theme.PrivacyExpenseTrackerTheme
import kotlinx.coroutines.delay

enum class AppNavState {
    SPLASH,
    WELCOME,
    CREATE_PROFILE,
    LOGIN,
    HOME,
    TRANSACTIONS,
    RECURRING_BILLS,
    BACKUP,
    SETTINGS
}

fun AppNavState.navDepth(): Int = when (this) {
    AppNavState.SPLASH -> 0
    AppNavState.WELCOME -> 1
    AppNavState.CREATE_PROFILE -> 2
    AppNavState.LOGIN -> 2
    AppNavState.HOME -> 3
    AppNavState.TRANSACTIONS -> 4
    AppNavState.RECURRING_BILLS -> 4
    AppNavState.BACKUP -> 4
    AppNavState.SETTINGS -> 4
}

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = AppDatabaseHelper.getInstance(this)
        handleSharedReceipt(intent)

        setContent {
            PrivacyExpenseTrackerTheme {
                ExpenseTrackerRootApp(dbHelper = dbHelper)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedReceipt(intent)
    }

    private fun handleSharedReceipt(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val detection = TransactionDetectionEngine.parseNotification(
                    packageName = "shared.receipt",
                    title = "Shared Receipt",
                    text = sharedText
                )
                if (detection.isTransaction && detection.transaction != null) {
                    val tx = detection.transaction.copy(status = TransactionStatus.SAVED)
                    dbHelper.insertTransaction(tx)
                    Toast.makeText(this, "✓ Captured ₹${tx.amount} to ${tx.merchant}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun ExpenseTrackerRootApp(dbHelper: AppDatabaseHelper) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("expense_tracker_session", Context.MODE_PRIVATE) }
    var currentScreen by remember { mutableStateOf(AppNavState.SPLASH) }
    var currentProfile by remember { mutableStateOf<LocalProfile?>(null) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }

    val pendingList = remember { mutableStateListOf<Transaction>() }
    val savedList = remember { mutableStateListOf<Transaction>() }
    val categoriesList = remember { mutableStateListOf<Category>() }
    val recurringBillsList = remember { mutableStateListOf<RecurringBill>() }

    fun refreshData() {
        pendingList.clear()
        pendingList.addAll(dbHelper.getPendingTransactions())

        savedList.clear()
        savedList.addAll(dbHelper.getSavedTransactions())

        categoriesList.clear()
        categoriesList.addAll(dbHelper.getCategories())

        recurringBillsList.clear()
        recurringBillsList.addAll(dbHelper.getRecurringBills())
    }

    fun proceedFromSplash() {
        if (currentScreen != AppNavState.SPLASH) return
        val profile = dbHelper.getProfile()
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (profile == null) {
            currentProfile = null
            currentScreen = AppNavState.WELCOME
        } else if (isLoggedIn) {
            currentProfile = profile
            currentScreen = AppNavState.HOME
        } else {
            currentProfile = profile
            currentScreen = AppNavState.LOGIN
        }
    }

    // Permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is needed for expense alerts", Toast.LENGTH_SHORT).show()
        }
    }

    // Storage Access Framework (SAF) Launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = dbHelper.exportDataAsJson()
                val salt = CryptoManager.generateSalt()
                val encrypted = CryptoManager.encryptPayload(json, "local_backup_key", salt)
                val backupObj = org.json.JSONObject().apply {
                    put("version", 1)
                    put("salt", salt)
                    put("payload", encrypted)
                }
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(backupObj.toString(2).toByteArray(Charsets.UTF_8))
                }
                backupStatusMessage = "Encrypted backup successfully saved to phone storage!"
            } catch (e: Exception) {
                backupStatusMessage = "Error saving backup: ${e.localizedMessage}"
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.reader(Charsets.UTF_8).readText()
                } ?: ""
                val success = try {
                    val backupObj = org.json.JSONObject(content)
                    val salt = backupObj.getString("salt")
                    val payload = backupObj.getString("payload")
                    val decrypted = CryptoManager.decryptPayload(payload, "local_backup_key", salt)
                    if (decrypted != null) {
                        dbHelper.importDataFromJson(decrypted)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    dbHelper.importDataFromJson(content)
                }
                if (success) {
                    backupStatusMessage = "Backup successfully restored & decrypted!"
                    refreshData()
                    val prof = dbHelper.getProfile()
                    if (prof != null) {
                        currentProfile = prof
                    }
                } else {
                    backupStatusMessage = "Failed to restore backup. Invalid or corrupt file."
                }
            } catch (e: Exception) {
                backupStatusMessage = "Error opening backup: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        refreshData()
        delay(1300L)
        proceedFromSplash()
    }

    // Apple-style fluid spring screen transition for every phone
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val isForward = targetState.navDepth() >= initialState.navDepth()
            if (isForward) {
                // Forward (Push): slide in from right, slide out with subtle parallax to left
                (slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                ) { fullWidth -> fullWidth } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                        ) { fullWidth -> -fullWidth / 3 } + fadeOut(animationSpec = tween(250))
                    )
            } else {
                // Backward (Pop): parent screen slides in from left, current screen slides off to right
                (slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                ) { fullWidth -> -fullWidth / 3 } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                        ) { fullWidth -> fullWidth } + fadeOut(animationSpec = tween(250))
                    )
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppNavState.SPLASH -> {
                OpeningSplashScreen(
                    onSplashFinished = { proceedFromSplash() }
                )
            }

            AppNavState.WELCOME -> {
                WelcomeScreen(
                    onCreateProfileClick = { currentScreen = AppNavState.CREATE_PROFILE },
                    onLoginClick = {
                        val profile = dbHelper.getProfile()
                        if (profile != null) {
                            currentProfile = profile
                            currentScreen = AppNavState.LOGIN
                        } else {
                            currentScreen = AppNavState.CREATE_PROFILE
                        }
                    },
                    onRestoreBackupClick = {
                        backupStatusMessage = null
                        importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                )
            }

            AppNavState.CREATE_PROFILE -> {
                CreateProfileScreen(
                    onCreateProfile = { name, password ->
                        val salt = CryptoManager.generateSalt()
                        val hash = CryptoManager.hashPassword(password, salt)
                        val profile = LocalProfile(displayName = name, passwordSalt = salt, passwordHash = hash)
                        dbHelper.saveProfile(profile)
                        currentProfile = profile
                        prefs.edit().putBoolean("is_logged_in", true).apply()
                        currentScreen = AppNavState.HOME
                    },
                    onBack = { currentScreen = AppNavState.WELCOME }
                )
            }

            AppNavState.LOGIN -> {
                LoginScreen(
                    profileName = currentProfile?.displayName ?: "User",
                    onLogin = { enteredPass ->
                        val p = currentProfile ?: dbHelper.getProfile()
                        if (p != null && CryptoManager.verifyPassword(enteredPass, p.passwordSalt, p.passwordHash)) {
                            currentProfile = p
                            prefs.edit().putBoolean("is_logged_in", true).apply()
                            currentScreen = AppNavState.HOME
                            true
                        } else {
                            false
                        }
                    },
                    onBack = { currentScreen = AppNavState.WELCOME },
                    onResetProfileClick = {
                        dbHelper.clearProfile()
                        currentProfile = null
                        prefs.edit().putBoolean("is_logged_in", false).apply()
                        currentScreen = AppNavState.WELCOME
                    }
                )
            }

            AppNavState.HOME -> {
                SignatureSplitHomeScreen(
                    userName = currentProfile?.displayName ?: "Akhil",
                    pendingCount = pendingList.size,
                    onNavigateToTransactions = {
                        refreshData()
                        currentScreen = AppNavState.TRANSACTIONS
                    },
                    onNavigateToRecurringBills = {
                        refreshData()
                        currentScreen = AppNavState.RECURRING_BILLS
                    },
                    onNavigateToBackup = {
                        backupStatusMessage = null
                        currentScreen = AppNavState.BACKUP
                    },
                    onNavigateToSettings = { currentScreen = AppNavState.SETTINGS },
                    onLogout = {
                        prefs.edit().putBoolean("is_logged_in", false).apply()
                        currentScreen = AppNavState.LOGIN
                    }
                )
            }

            AppNavState.RECURRING_BILLS -> {
                RecurringBillsScreen(
                    bills = recurringBillsList,
                    onAddBill = { newBill ->
                        dbHelper.addRecurringBill(newBill)
                        refreshData()
                    },
                    onDeleteBill = { id ->
                        dbHelper.deleteRecurringBill(id)
                        refreshData()
                    },
                    onMarkAsPaid = { bill ->
                        val tx = Transaction(
                            amount = bill.amount,
                            merchant = bill.title,
                            categoryName = bill.categoryName,
                            type = TransactionType.DEBIT,
                            status = TransactionStatus.SAVED,
                            sourceApp = "Recurring Bill",
                            transactionTime = System.currentTimeMillis(),
                            detectedTime = System.currentTimeMillis()
                        )
                        dbHelper.insertTransaction(tx)
                        refreshData()
                        Toast.makeText(context, "✓ Logged ₹${bill.amount} for ${bill.title}", Toast.LENGTH_SHORT).show()
                    },
                    onBack = {
                        refreshData()
                        currentScreen = AppNavState.HOME
                    }
                )
            }

            AppNavState.TRANSACTIONS -> {
                CustomTransactionsScreen(
                    pendingTransactions = pendingList,
                    savedTransactions = savedList,
                    categories = categoriesList,
                    onCategorizeTransaction = { txId, catId, catName ->
                        dbHelper.updateTransactionCategory(txId, catId, catName)
                        refreshData()
                    },
                    onDeleteTransaction = { txId ->
                        dbHelper.deleteTransaction(txId)
                        refreshData()
                    },
                    onUpdateTransaction = { updatedTx ->
                        dbHelper.updateTransaction(updatedTx)
                        refreshData()
                    },
                    onAddManualTransaction = { tx ->
                        dbHelper.insertTransaction(tx)
                        refreshData()
                    },
                    onBackToHome = {
                        refreshData()
                        currentScreen = AppNavState.HOME
                    }
                )
            }

            AppNavState.BACKUP -> {
                BackupScreen(
                    onBack = { currentScreen = AppNavState.HOME },
                    onCreateBackupClick = { exportBackupLauncher.launch("privacy_expense_backup.json") },
                    onRestoreBackupClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                    onClearAllDataClick = {
                        dbHelper.clearAllTransactions()
                        refreshData()
                        backupStatusMessage = "All transactions deleted from database."
                    },
                    statusMessage = backupStatusMessage
                )
            }

            AppNavState.SETTINGS -> {
                SettingsScreen(
                    categories = categoriesList,
                    onAddCategory = { name, icon ->
                        dbHelper.addCategory(Category(name = name, iconKey = icon, colorHex = "#F4D3A1", sortOrder = categoriesList.size))
                        refreshData()
                    },
                    onDeleteCategory = { id ->
                        dbHelper.deleteCategory(id)
                        refreshData()
                    },
                    onToggleCategoryNotification = { id, enabled ->
                        dbHelper.updateCategoryNotification(id, enabled)
                        refreshData()
                    },
                    onOpenNotificationSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            Toast.makeText(context, "Notification permission already active", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBack = { currentScreen = AppNavState.HOME }
                )
            }
        }
    }
}
