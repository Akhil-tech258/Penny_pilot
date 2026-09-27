package com.privacyexpense.tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyexpense.tracker.data.model.RecurringBill
import com.privacyexpense.tracker.ui.theme.TableBorderBlack
import com.privacyexpense.tracker.ui.theme.TextBlack
import com.privacyexpense.tracker.ui.theme.TextSecondary

@Composable
fun RecurringBillsScreen(
    bills: List<RecurringBill>,
    onAddBill: (RecurringBill) -> Unit,
    onDeleteBill: (Long) -> Unit,
    onMarkAsPaid: (RecurringBill) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }

    val totalMonthlyCommitment = bills.filter { it.isActive }.sumOf { it.amount }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack,
                    modifier = Modifier.clickable { onBack() }
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TableBorderBlack)
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ Add Bill",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Recurring Bills & Subscriptions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Track fixed commitments without waiting for notifications",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, TableBorderBlack, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Monthly Bills",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${String.format("%.2f", totalMonthlyCommitment)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                    }
                    Text(
                        text = "${bills.size} Active Commitments",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📅", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recurring bills set up yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                        Text(
                            text = "Add Rent, Wi-Fi, Netflix, Gym, or EMIs to track easily",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bills) { bill ->
                        RecurringBillItem(
                            bill = bill,
                            onMarkAsPaid = { onMarkAsPaid(bill) },
                            onDelete = { onDeleteBill(bill.id) }
                        )
                    }
                }
            }
        }

        // Add Bill Dialog
        if (showAddDialog) {
            AddRecurringBillDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { newBill ->
                    onAddBill(newBill)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun RecurringBillItem(
    bill: RecurringBill,
    onMarkAsPaid: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TableBorderBlack, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = "₹${String.format("%.2f", bill.amount)} • Due on ${bill.dueDay}th every month",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Category: ${bill.categoryName}",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // 1-Tap Mark as Paid
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF059669))
                        .clickable(onClick = onMarkAsPaid)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Mark Paid",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Delete
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2))
                        .clickable(onClick = onDelete)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddRecurringBillDialog(
    onDismiss: () -> Unit,
    onConfirm: (RecurringBill) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDayText by remember { mutableStateOf("1") }
    var categoryName by remember { mutableStateOf("Bills") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Recurring Bill",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bill Name") },
                    placeholder = { Text("e.g. Broadband, Rent, Netflix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("e.g. 799") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text("Day of Month (1-31)") },
                    placeholder = { Text("e.g. 5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g. Utilities, Housing") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TableBorderBlack)
                    .clickable {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val day = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1
                        if (title.isNotBlank() && amt > 0) {
                            onConfirm(
                                RecurringBill(
                                    title = title.trim(),
                                    amount = amt,
                                    dueDay = day,
                                    categoryName = categoryName.ifBlank { "Bills" }
                                )
                            )
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "Save Bill", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Text(
                text = "Cancel",
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = TextSecondary
            )
        }
    )
}
