package com.privacyexpense.tracker.data.model

data class RecurringBill(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDay: Int, // 1 to 31
    val categoryName: String = "Bills",
    val isActive: Boolean = true
)
