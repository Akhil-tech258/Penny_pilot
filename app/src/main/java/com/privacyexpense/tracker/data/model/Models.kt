package com.privacyexpense.tracker.data.model

enum class TransactionType {
    DEBIT,
    CREDIT,
    REFUND,
    TRANSFER
}

enum class TransactionStatus {
    PENDING,
    SAVED,
    DELETED
}

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val currency: String = "INR",
    val type: TransactionType = TransactionType.DEBIT,
    val merchant: String,
    val description: String = "",
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val sourceApp: String = "",
    val sourcePackage: String = "",
    val transactionTime: Long = System.currentTimeMillis(),
    val detectedTime: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.PENDING
)

data class Category(
    val id: Long = 0,
    val name: String,
    val iconKey: String = "",
    val colorHex: String = "#F4D3A1",
    val sortOrder: Int = 0,
    val isEnabled: Boolean = true,
    val isNotificationEnabled: Boolean = true
)

data class LocalProfile(
    val id: Long = 1,
    val displayName: String,
    val passwordSalt: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class SplitRecord(
    val id: Long = 0,
    val transactionId: Long,
    val totalAmount: Double,
    val myShare: Double,
    val owedAmount: Double,
    val friendNames: String,
    val isSettled: Boolean = false
)
