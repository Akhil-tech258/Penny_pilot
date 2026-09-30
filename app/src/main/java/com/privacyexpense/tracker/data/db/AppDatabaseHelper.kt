package com.privacyexpense.tracker.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.privacyexpense.tracker.data.model.Category
import com.privacyexpense.tracker.data.model.LocalProfile
import com.privacyexpense.tracker.data.model.RecurringBill
import com.privacyexpense.tracker.data.model.SplitRecord
import com.privacyexpense.tracker.data.model.Transaction
import com.privacyexpense.tracker.data.model.TransactionStatus
import com.privacyexpense.tracker.data.model.TransactionType

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "privacy_expense_tracker.db"
        const val DATABASE_VERSION = 1

        private var instance: AppDatabaseHelper? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabaseHelper {
            if (instance == null) {
                instance = AppDatabaseHelper(context.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Local Profile table
        db.execSQL(
            """
            CREATE TABLE local_profile (
                id INTEGER PRIMARY KEY,
                display_name TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Categories table
        db.execSQL(
            """
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                icon_key TEXT NOT NULL,
                color_hex TEXT NOT NULL,
                sort_order INTEGER NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                is_notification_enabled INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        // Transactions table
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                currency TEXT NOT NULL DEFAULT 'INR',
                type TEXT NOT NULL,
                merchant TEXT NOT NULL,
                description TEXT DEFAULT '',
                category_id INTEGER,
                category_name TEXT,
                source_app TEXT DEFAULT '',
                source_package TEXT DEFAULT '',
                transaction_time INTEGER NOT NULL,
                detected_time INTEGER NOT NULL,
                status TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Detection Rules table
        db.execSQL(
            """
            CREATE TABLE detection_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_package TEXT,
                pattern TEXT NOT NULL,
                rule_type TEXT NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        // Recurring Bills table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recurring_bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                due_day INTEGER NOT NULL,
                category_name TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        // Split with Friends table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS split_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                transaction_id INTEGER,
                total_amount REAL NOT NULL,
                my_share REAL NOT NULL,
                owed_amount REAL NOT NULL,
                friend_names TEXT NOT NULL,
                is_settled INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // Seed Default Categories matching Storyboard
        seedCategories(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recurring_bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                due_day INTEGER NOT NULL,
                category_name TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS split_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                transaction_id INTEGER,
                total_amount REAL NOT NULL,
                my_share REAL NOT NULL,
                owed_amount REAL NOT NULL,
                friend_names TEXT NOT NULL,
                is_settled INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun seedCategories(db: SQLiteDatabase) {
        val defaultCategories = listOf(
            Triple("Food", "fork.knife", "#F97316"),       // Orange
            Triple("Groceries", "cart", "#10B981"),        // Green
            Triple("Clothes", "tshirt", "#3B82F6"),        // Blue
            Triple("Travel", "airplane", "#06B6D4"),       // Cyan
            Triple("College", "graduationcap", "#EF4444"), // Red
            Triple("Others", "sparkles", "#8B5CF6")        // Purple
        )

        defaultCategories.forEachIndexed { index, (name, icon, color) ->
            val values = ContentValues().apply {
                put("name", name)
                put("icon_key", icon)
                put("color_hex", color)
                put("sort_order", index)
                put("is_enabled", 1)
                put("is_notification_enabled", 1)
            }
            db.insert("categories", null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema migrations
    }

    // --- Profile Operations ---
    fun getProfile(): LocalProfile? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, display_name, password_salt, password_hash, created_at FROM local_profile LIMIT 1", null)
        cursor.use {
            if (it.moveToFirst()) {
                return LocalProfile(
                    id = it.getLong(0),
                    displayName = it.getString(1),
                    passwordSalt = it.getString(2),
                    passwordHash = it.getString(3),
                    createdAt = it.getLong(4)
                )
            }
        }
        return null
    }

    fun saveProfile(profile: LocalProfile): Boolean {
        val db = writableDatabase
        db.delete("local_profile", null, null)
        val values = ContentValues().apply {
            put("id", profile.id)
            put("display_name", profile.displayName)
            put("password_salt", profile.passwordSalt)
            put("password_hash", profile.passwordHash)
            put("created_at", profile.createdAt)
        }
        return db.insert("local_profile", null, values) > 0
    }

    fun clearProfile() {
        val db = writableDatabase
        db.delete("local_profile", null, null)
    }

    // --- Category Operations ---
    fun getCategories(): List<Category> {
        val list = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, name, icon_key, color_hex, sort_order, is_enabled, is_notification_enabled FROM categories ORDER BY sort_order ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Category(
                        id = it.getLong(0),
                        name = it.getString(1),
                        iconKey = it.getString(2),
                        colorHex = it.getString(3),
                        sortOrder = it.getInt(4),
                        isEnabled = it.getInt(5) == 1,
                        isNotificationEnabled = it.getInt(6) == 1
                    )
                )
            }
        }
        return list
    }

    // --- Transaction Operations ---
    fun insertTransaction(transaction: Transaction): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("amount", transaction.amount)
            put("currency", transaction.currency)
            put("type", transaction.type.name)
            put("merchant", transaction.merchant)
            put("description", transaction.description)
            put("category_id", transaction.categoryId)
            put("category_name", transaction.categoryName)
            put("source_app", transaction.sourceApp)
            put("source_package", transaction.sourcePackage)
            put("transaction_time", transaction.transactionTime)
            put("detected_time", transaction.detectedTime)
            put("status", transaction.status.name)
        }
        return db.insert("transactions", null, values)
    }

    fun updateTransactionCategory(
        transactionId: Long,
        categoryId: Long?,
        categoryName: String,
        description: String? = null
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (categoryId != null) {
                put("category_id", categoryId)
            }
            put("category_name", categoryName)
            if (description != null) {
                put("description", description)
            }
            put("status", TransactionStatus.SAVED.name)
        }
        return db.update("transactions", values, "id = ?", arrayOf(transactionId.toString())) > 0
    }

    fun getPendingTransactions(): List<Transaction> {
        return getTransactionsByStatus(TransactionStatus.PENDING)
    }

    fun getSavedTransactions(): List<Transaction> {
        return getTransactionsByStatus(TransactionStatus.SAVED)
    }

    private fun getTransactionsByStatus(status: TransactionStatus): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT id, amount, currency, type, merchant, description, category_id, category_name, 
                   source_app, source_package, transaction_time, detected_time, status 
            FROM transactions 
            WHERE status = ? 
            ORDER BY transaction_time DESC
            """.trimIndent(),
            arrayOf(status.name)
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Transaction(
                        id = it.getLong(0),
                        amount = it.getDouble(1),
                        currency = it.getString(2),
                        type = try { TransactionType.valueOf(it.getString(3)) } catch (e: Exception) { TransactionType.DEBIT },
                        merchant = it.getString(4),
                        description = it.getString(5) ?: "",
                        categoryId = if (it.isNull(6)) null else it.getLong(6),
                        categoryName = it.getString(7),
                        sourceApp = it.getString(8) ?: "",
                        sourcePackage = it.getString(9) ?: "",
                        transactionTime = it.getLong(10),
                        detectedTime = it.getLong(11),
                        status = try { TransactionStatus.valueOf(it.getString(12)) } catch (e: Exception) { TransactionStatus.PENDING }
                    )
                )
            }
        }
        return list
    }

    fun isDuplicateOrEnrich(incoming: Transaction, withinMs: Long = 180_000): Boolean {
        val db = writableDatabase
        val minTime = System.currentTimeMillis() - withinMs
        val cursor = db.rawQuery(
            """
            SELECT id, amount, type, merchant, description, source_app, detected_time 
            FROM transactions 
            WHERE type = ? AND ABS(amount - ?) < 0.01 AND detected_time > ?
            ORDER BY detected_time DESC
            """.trimIndent(),
            arrayOf(incoming.type.name, incoming.amount.toString(), minTime.toString())
        )

        var isDuplicate = false
        cursor.use {
            while (it.moveToNext()) {
                val existingId = it.getLong(0)
                val existingMerchant = it.getString(3) ?: ""
                val existingDesc = it.getString(4) ?: ""
                val existingSource = it.getString(5) ?: ""

                // 1. Reference Number match (if both have UPI reference numbers and they match)
                val incomingRef = extractRef(incoming.description)
                val existingRef = extractRef(existingDesc)
                if (incomingRef != null && existingRef != null && incomingRef == existingRef) {
                    isDuplicate = true
                    break
                }

                // 2. Cross-Channel Deduplication: UPI App vs Bank SMS
                val isIncomingUpi = isUpiApp(incoming.sourceApp)
                val isExistingUpi = isUpiApp(existingSource)
                val isIncomingSms = isSmsApp(incoming.sourceApp)
                val isExistingSms = isSmsApp(existingSource)

                if ((isIncomingUpi && isExistingSms) || (isIncomingSms && isExistingUpi)) {
                    isDuplicate = true

                    // If incoming is from a clean UPI app (GPay, PhonePe) and existing is from Bank SMS with raw VPA/text,
                    // upgrade the existing transaction with the cleaner merchant name and source app!
                    if (isIncomingUpi && incoming.merchant.isNotBlank() && incoming.merchant != "Unknown Merchant") {
                        val values = ContentValues().apply {
                            put("merchant", incoming.merchant)
                            put("source_app", incoming.sourceApp)
                        }
                        db.update("transactions", values, "id = ?", arrayOf(existingId.toString()))
                    }
                    break
                }

                // 3. Merchant Name similarity or Exact Match
                val m1 = incoming.merchant.lowercase().replace(Regex("[^a-z0-9]"), "")
                val m2 = existingMerchant.lowercase().replace(Regex("[^a-z0-9]"), "")
                if (m1 == m2 || (m1.length >= 4 && m2.contains(m1)) || (m2.length >= 4 && m1.contains(m2))) {
                    isDuplicate = true
                    break
                }

                // 4. Same source app sent duplicate alert within window
                if (incoming.sourceApp.isNotBlank() && incoming.sourceApp.equals(existingSource, ignoreCase = true)) {
                    isDuplicate = true
                    break
                }
            }
        }

        return isDuplicate
    }

    private fun extractRef(desc: String): String? {
        val m = java.util.regex.Pattern.compile("""(?:Ref|UTR)[:\s]*(\d{10,14})""", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(desc)
        return if (m.find()) m.group(1) else null
    }

    private fun isUpiApp(sourceApp: String): Boolean {
        val s = sourceApp.lowercase()
        return s.contains("google pay") || s.contains("phonepe") || s.contains("paytm") || s.contains("cred") || s.contains("bhim")
    }

    private fun isSmsApp(sourceApp: String): Boolean {
        val s = sourceApp.lowercase()
        return s.contains("sms") || s.contains("message") || s.contains("messaging") || s.contains("bank")
    }

    fun isDuplicateTransaction(amount: Double, merchant: String, withinMs: Long = 60_000): Boolean {
        val db = readableDatabase
        val minTime = System.currentTimeMillis() - withinMs
        val cursor = db.rawQuery(
            "SELECT id FROM transactions WHERE amount = ? AND merchant = ? AND detected_time > ? LIMIT 1",
            arrayOf(amount.toString(), merchant, minTime.toString())
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun deleteTransaction(transactionId: Long): Boolean {
        val db = writableDatabase
        return db.delete("transactions", "id = ?", arrayOf(transactionId.toString())) > 0
    }

    fun updateTransaction(transaction: Transaction): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("amount", transaction.amount)
            put("merchant", transaction.merchant)
            put("category_id", transaction.categoryId)
            put("category_name", transaction.categoryName)
            put("description", transaction.description)
            put("type", transaction.type.name)
            put("status", transaction.status.name)
        }
        return db.update("transactions", values, "id = ?", arrayOf(transaction.id.toString())) > 0
    }

    fun addCategory(category: Category): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", category.name)
            put("icon_key", category.iconKey)
            put("color_hex", category.colorHex)
            put("sort_order", category.sortOrder)
            put("is_enabled", if (category.isEnabled) 1 else 0)
            put("is_notification_enabled", if (category.isNotificationEnabled) 1 else 0)
        }
        return db.insert("categories", null, values)
    }

    fun updateCategoryNotification(categoryId: Long, enabled: Boolean): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_notification_enabled", if (enabled) 1 else 0)
        }
        return db.update("categories", values, "id = ?", arrayOf(categoryId.toString())) > 0
    }

    fun deleteCategory(categoryId: Long): Boolean {
        val db = writableDatabase
        return db.delete("categories", "id = ?", arrayOf(categoryId.toString())) > 0
    }

    fun clearAllTransactions() {
        val db = writableDatabase
        db.delete("transactions", null, null)
    }

    // Export all local data to clean JSON for encrypted backup
    fun exportDataAsJson(): String {
        val profile = getProfile()
        val categories = getCategories()
        val saved = getSavedTransactions()
        val pending = getPendingTransactions()

        val json = org.json.JSONObject()
        profile?.let {
            val profObj = org.json.JSONObject().apply {
                put("displayName", it.displayName)
                put("passwordSalt", it.passwordSalt)
                put("passwordHash", it.passwordHash)
                put("createdAt", it.createdAt)
            }
            json.put("profile", profObj)
        }

        val catArray = org.json.JSONArray()
        for (c in categories) {
            val obj = org.json.JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("iconKey", c.iconKey)
                put("colorHex", c.colorHex)
                put("sortOrder", c.sortOrder)
                put("isEnabled", c.isEnabled)
                put("isNotificationEnabled", c.isNotificationEnabled)
            }
            catArray.put(obj)
        }
        json.put("categories", catArray)

        val txArray = org.json.JSONArray()
        val allTx = saved + pending
        for (tx in allTx) {
            val obj = org.json.JSONObject().apply {
                put("amount", tx.amount)
                put("currency", tx.currency)
                put("type", tx.type.name)
                put("merchant", tx.merchant)
                put("description", tx.description)
                put("categoryId", tx.categoryId ?: -1L)
                put("categoryName", tx.categoryName ?: "")
                put("sourceApp", tx.sourceApp)
                put("sourcePackage", tx.sourcePackage)
                put("transactionTime", tx.transactionTime)
                put("detectedTime", tx.detectedTime)
                put("status", tx.status.name)
            }
            txArray.put(obj)
        }
        json.put("transactions", txArray)
        json.put("exportedAt", System.currentTimeMillis())

        return json.toString()
    }

    // Restore data from decrypted JSON
    fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val db = writableDatabase
            db.beginTransaction()
            try {
                // Restore profile if present
                if (json.has("profile")) {
                    val p = json.getJSONObject("profile")
                    val profile = LocalProfile(
                        displayName = p.getString("displayName"),
                        passwordSalt = p.getString("passwordSalt"),
                        passwordHash = p.getString("passwordHash"),
                        createdAt = p.getLong("createdAt")
                    )
                    saveProfile(profile)
                }

                // Restore categories if present
                if (json.has("categories")) {
                    val catArr = json.getJSONArray("categories")
                    db.delete("categories", null, null)
                    for (i in 0 until catArr.length()) {
                        val c = catArr.getJSONObject(i)
                        val values = ContentValues().apply {
                            put("id", c.getLong("id"))
                            put("name", c.getString("name"))
                            put("icon_key", c.optString("iconKey", "sparkles"))
                            put("color_hex", c.optString("colorHex", "#F4D3A1"))
                            put("sort_order", c.optInt("sortOrder", i))
                            put("is_enabled", if (c.optBoolean("isEnabled", true)) 1 else 0)
                            put("is_notification_enabled", if (c.optBoolean("isNotificationEnabled", true)) 1 else 0)
                        }
                        db.insert("categories", null, values)
                    }
                }

                // Restore transactions
                if (json.has("transactions")) {
                    val txArr = json.getJSONArray("transactions")
                    db.delete("transactions", null, null)
                    for (i in 0 until txArr.length()) {
                        val t = txArr.getJSONObject(i)
                        val values = ContentValues().apply {
                            put("amount", t.getDouble("amount"))
                            put("currency", t.optString("currency", "INR"))
                            put("type", t.optString("type", "DEBIT"))
                            put("merchant", t.getString("merchant"))
                            put("description", t.optString("description", ""))
                            val cId = t.optLong("categoryId", -1L)
                            if (cId > 0) put("category_id", cId)
                            put("category_name", t.optString("categoryName", ""))
                            put("source_app", t.optString("sourceApp", ""))
                            put("source_package", t.optString("sourcePackage", ""))
                            put("transaction_time", t.optLong("transactionTime", System.currentTimeMillis()))
                            put("detected_time", t.optLong("detectedTime", System.currentTimeMillis()))
                            put("status", t.optString("status", "SAVED"))
                        }
                        db.insert("transactions", null, values)
                    }
                }

                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Recurring Bills Operations ---
    fun getRecurringBills(): List<RecurringBill> {
        val list = mutableListOf<RecurringBill>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, title, amount, due_day, category_name, is_active FROM recurring_bills ORDER BY due_day ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    RecurringBill(
                        id = it.getLong(0),
                        title = it.getString(1),
                        amount = it.getDouble(2),
                        dueDay = it.getInt(3),
                        categoryName = it.getString(4),
                        isActive = it.getInt(5) == 1
                    )
                )
            }
        }
        return list
    }

    fun addRecurringBill(bill: RecurringBill): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", bill.title)
            put("amount", bill.amount)
            put("due_day", bill.dueDay)
            put("category_name", bill.categoryName)
            put("is_active", if (bill.isActive) 1 else 0)
        }
        return db.insert("recurring_bills", null, values)
    }

    fun deleteRecurringBill(id: Long): Boolean {
        val db = writableDatabase
        return db.delete("recurring_bills", "id = ?", arrayOf(id.toString())) > 0
    }

    // --- Split with Friends Operations ---
    fun getUnsettledSplits(): List<SplitRecord> {
        val list = mutableListOf<SplitRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, transaction_id, total_amount, my_share, owed_amount, friend_names, is_settled FROM split_records WHERE is_settled = 0", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SplitRecord(
                        id = it.getLong(0),
                        transactionId = it.getLong(1),
                        totalAmount = it.getDouble(2),
                        myShare = it.getDouble(3),
                        owedAmount = it.getDouble(4),
                        friendNames = it.getString(5),
                        isSettled = it.getInt(6) == 1
                    )
                )
            }
        }
        return list
    }

    fun saveSplit(record: SplitRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("transaction_id", record.transactionId)
            put("total_amount", record.totalAmount)
            put("my_share", record.myShare)
            put("owed_amount", record.owedAmount)
            put("friend_names", record.friendNames)
            put("is_settled", if (record.isSettled) 1 else 0)
        }
        return db.insert("split_records", null, values)
    }

    fun settleSplit(id: Long): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("is_settled", 1) }
        return db.update("split_records", values, "id = ?", arrayOf(id.toString())) > 0
    }

    fun getTotalOwedToMe(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM(owed_amount) FROM split_records WHERE is_settled = 0", null)
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                return it.getDouble(0)
            }
        }
        return 0.0
    }

    fun getTodayDebitSum(): Double {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND status = 'SAVED' AND transaction_time >= ?",
            arrayOf(startOfDay.toString())
        )
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                return it.getDouble(0)
            }
        }
        return 0.0
    }

    fun getTodayCreditSum(): Double {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND status = 'SAVED' AND transaction_time >= ?",
            arrayOf(startOfDay.toString())
        )
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                return it.getDouble(0)
            }
        }
        return 0.0
    }

    fun getTodayTransactionCount(): Int {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM transactions WHERE status = 'SAVED' AND transaction_time >= ?",
            arrayOf(startOfDay.toString())
        )
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                return it.getInt(0)
            }
        }
        return 0
    }
}
