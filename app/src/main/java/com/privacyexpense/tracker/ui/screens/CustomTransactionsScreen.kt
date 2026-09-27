package com.privacyexpense.tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyexpense.tracker.data.model.Category
import com.privacyexpense.tracker.data.model.Transaction
import com.privacyexpense.tracker.data.model.TransactionStatus
import com.privacyexpense.tracker.data.model.TransactionType
import com.privacyexpense.tracker.ui.theme.AccentGold
import com.privacyexpense.tracker.ui.theme.AvatarPeach
import com.privacyexpense.tracker.ui.theme.CatClothes
import com.privacyexpense.tracker.ui.theme.CatCollege
import com.privacyexpense.tracker.ui.theme.CatFood
import com.privacyexpense.tracker.ui.theme.CatGroceries
import com.privacyexpense.tracker.ui.theme.CatOthers
import com.privacyexpense.tracker.ui.theme.CatTravel
import com.privacyexpense.tracker.ui.theme.GlowAmber
import com.privacyexpense.tracker.ui.theme.TableBorderBlack
import com.privacyexpense.tracker.ui.theme.TextBlack
import com.privacyexpense.tracker.ui.theme.TextSecondary

@Composable
fun CustomTransactionsScreen(
    pendingTransactions: List<Transaction>,
    savedTransactions: List<Transaction>,
    categories: List<Category>,
    onCategorizeTransaction: (transactionId: Long, categoryId: Long, categoryName: String) -> Unit,
    onDeleteTransaction: (transactionId: Long) -> Unit,
    onUpdateTransaction: (Transaction) -> Unit = {},
    onAddManualTransaction: (Transaction) -> Unit,
    onBackToHome: () -> Unit
) {
    // Intercept back button to return to Home
    BackHandler {
        onBackToHome()
    }

    var selectedPendingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var viewingTransactionDetail by remember { mutableStateOf<Transaction?>(null) }
    var isAddingManualTransaction by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") } // "All", "Debits", "Credits"

    val debitedTransactions = savedTransactions.filter { tx ->
        tx.type == TransactionType.DEBIT &&
                (tx.merchant.contains(searchQuery, ignoreCase = true) ||
                        (tx.categoryName ?: "").contains(searchQuery, ignoreCase = true) ||
                        tx.amount.toString().contains(searchQuery))
    }

    val creditedTransactions = savedTransactions.filter { tx ->
        tx.type == TransactionType.CREDIT &&
                (tx.merchant.contains(searchQuery, ignoreCase = true) ||
                        (tx.categoryName ?: "").contains(searchQuery, ignoreCase = true) ||
                        tx.amount.toString().contains(searchQuery))
    }

    val totalSpent = savedTransactions
        .filter { it.type == TransactionType.DEBIT }
        .sumOf { it.amount }

    val totalCredited = savedTransactions
        .filter { it.type == TransactionType.CREDIT }
        .sumOf { it.amount }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Ambient warm glow in top-left matching user's Canva design
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GlowAmber.copy(alpha = 0.50f),
                            GlowAmber.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Home",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack,
                    modifier = Modifier.clickable(onClick = onBackToHome)
                )

                // Quick Add Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, TableBorderBlack, RoundedCornerShape(8.dp))
                        .clickable { isAddingManualTransaction = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ Add",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TITLE: TRANSCTIONS (Exact Canva design title)
            Text(
                text = "TRANSCTIONS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ELEVATED DESIGN: Total Spent & Credited Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
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
                            text = "Total Spent",
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${formatAmount(totalSpent)}",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Received",
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextSecondary
                        )
                        Text(
                            text = "+₹${formatAmount(totalCredited)}",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: Pending (Italic font from Canva screen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pending",
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )

                if (pendingTransactions.isNotEmpty()) {
                    Text(
                        text = "${pendingTransactions.size} require category",
                        fontSize = 11.sp,
                        color = Color(0xFFD97706)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pendingTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pending transactions. All caught up!",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextSecondary
                    )
                }
            } else {
                pendingTransactions.forEach { tx ->
                    PendingCard(
                        transaction = tx,
                        onCategorizeClick = { selectedPendingTransaction = tx }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: Last Transactions (Bold subheader from Canva screen)
            Text(
                text = "Last Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Clean Search Bar & Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search merchant or reason...", fontSize = 12.sp, color = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TableBorderBlack,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips (All, Debits, Credits)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(text = "All", isSelected = activeFilter == "All") { activeFilter = "All" }
                FilterChip(text = "Debits (${debitedTransactions.size})", isSelected = activeFilter == "Debits") { activeFilter = "Debits" }
                FilterChip(text = "Credits (${creditedTransactions.size})", isSelected = activeFilter == "Credits") { activeFilter = "Credits" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3: TWO BORDERED TABLES (Debited & Credited)
            if (activeFilter == "All" || activeFilter == "Debits") {
                BorderedTransactionTable(
                    title = "Debited (Expenses)",
                    partyHeader = "Debited to",
                    isCredit = false,
                    transactions = debitedTransactions,
                    onRowClick = { tx -> viewingTransactionDetail = tx }
                )
            }

            if (activeFilter == "All") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (activeFilter == "All" || activeFilter == "Credits") {
                BorderedTransactionTable(
                    title = "Credited (Income)",
                    partyHeader = "Credited from",
                    isCredit = true,
                    transactions = creditedTransactions,
                    onRowClick = { tx -> viewingTransactionDetail = tx }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // SLIDE-UP CATEGORY SELECTION SHEET (Storyboard Screen 10)
        AnimatedVisibility(
            visible = selectedPendingTransaction != null,
            enter = fadeIn() + slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            selectedPendingTransaction?.let { tx ->
                CategorySelectionSheet(
                    transaction = tx,
                    categories = categories,
                    onSelectCategory = { cat ->
                        onCategorizeTransaction(tx.id, cat.id, cat.name)
                        selectedPendingTransaction = null
                    },
                    onDismiss = { selectedPendingTransaction = null }
                )
            }
        }

        // TRANSACTION DETAIL & EDIT / DELETE MODAL
        AnimatedVisibility(
            visible = viewingTransactionDetail != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            viewingTransactionDetail?.let { tx ->
                TransactionDetailDialog(
                    transaction = tx,
                    categories = categories,
                    onUpdate = { updatedTx ->
                        onUpdateTransaction(updatedTx)
                        viewingTransactionDetail = null
                    },
                    onDelete = {
                        onDeleteTransaction(tx.id)
                        viewingTransactionDetail = null
                    },
                    onDismiss = { viewingTransactionDetail = null }
                )
            }
        }

        // MANUAL ADD TRANSACTION MODAL
        AnimatedVisibility(
            visible = isAddingManualTransaction,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AddTransactionDialog(
                categories = categories,
                onAdd = { newTx ->
                    onAddManualTransaction(newTx)
                    isAddingManualTransaction = false
                },
                onDismiss = { isAddingManualTransaction = false }
            )
        }
    }
}

@Composable
private fun PillBadge(text: String, isHighlight: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHighlight) Color(0xFFFEF3C7) else Color(0xFFE5E7EB))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) Color(0xFFB45309) else TextBlack
        )
    }
}

@Composable
private fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, if (isSelected) TableBorderBlack else Color.LightGray, RoundedCornerShape(6.dp))
            .background(if (isSelected) TableBorderBlack else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else TextBlack
        )
    }
}

@Composable
private fun PendingCard(
    transaction: Transaction,
    onCategorizeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    text = "₹${formatAmount(transaction.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = transaction.merchant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Needs category",
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFFD97706)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TableBorderBlack)
                    .clickable(onClick = onCategorizeClick)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Categorize",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BorderedTransactionTable(
    title: String,
    partyHeader: String,
    isCredit: Boolean,
    transactions: List<Transaction>,
    onRowClick: (Transaction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Table Subheader Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isCredit) Color(0xFF059669) else TableBorderBlack)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }
            Text(
                text = "${transactions.size} records",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Outer bold border
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, TableBorderBlack)
                .background(Color.White)
        ) {
            // Table Header: amount | Date & Time | partyHeader (Debited to / Credited from) | Reasons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(Color(0xFFFAFAFA)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "amount",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(0.25f)
                        .padding(vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(TableBorderBlack)
                )
                Text(
                    text = "Date & Time",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(0.24f)
                        .padding(vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(TableBorderBlack)
                )
                Text(
                    text = partyHeader,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(0.28f)
                        .padding(vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(TableBorderBlack)
                )
                Text(
                    text = "Reasons",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(0.23f)
                        .padding(vertical = 8.dp)
                )
            }

            // Horizontal divider under header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(TableBorderBlack)
            )

            // Transactions Rows
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isCredit) "No credited transactions found" else "No debited transactions found",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                        Text(
                            text = if (isCredit) "Incoming UPI/bank credits will appear here" else "Payments from Google Pay/PhonePe will appear here",
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                val dateFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

                transactions.forEach { tx ->
                    val txDate = java.util.Date(tx.transactionTime)
                    val dateStr = if (tx.transactionTime > 0) dateFormat.format(txDate) else "Today"
                    val timeStr = if (tx.transactionTime > 0) timeFormat.format(txDate) else ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clickable { onRowClick(tx) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Amount with credit/debit indicator
                        Text(
                            text = (if (isCredit) "+ " else "") + formatAmount(tx.amount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCredit) Color(0xFF059669) else TextBlack,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(0.25f)
                                .padding(vertical = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(TableBorderBlack)
                        )
                        // Date & Time Column
                        Column(
                            modifier = Modifier
                                .weight(0.24f)
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dateStr,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextBlack,
                                textAlign = TextAlign.Center
                            )
                            if (timeStr.isNotEmpty()) {
                                Text(
                                    text = timeStr,
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(TableBorderBlack)
                        )
                        Text(
                            text = tx.merchant,
                            fontSize = 11.sp,
                            color = TextBlack,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(0.28f)
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(TableBorderBlack)
                        )
                        Text(
                            text = tx.categoryName ?: "Others",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(0.23f)
                                .padding(vertical = 8.dp)
                        )
                    }

                    // Row horizontal divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TableBorderBlack)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySelectionSheet(
    transaction: Transaction,
    categories: List<Category>,
    onSelectCategory: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Category",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = "₹${formatAmount(transaction.amount)} • ${transaction.merchant}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 6 Category Circular Icons
                val defaultCategoryDisplay = listOf(
                    Triple("Food", CatFood, "🍴"),
                    Triple("Groceries", CatGroceries, "🛍️"),
                    Triple("Clothes", CatClothes, "👕"),
                    Triple("Travel", CatTravel, "✈️"),
                    Triple("College", CatCollege, "🎓"),
                    Triple("Others", CatOthers, "✨")
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val idx = row * 3 + col
                                val (name, color, emoji) = defaultCategoryDisplay[idx]
                                val matchedCat = categories.find { it.name.equals(name, true) }
                                    ?: Category(id = (idx + 1).toLong(), name = name)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onSelectCategory(matchedCat) }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.20f))
                                            .border(1.5.dp, color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextBlack
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: Transaction,
    categories: List<Category>,
    onUpdate: (Transaction) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var merchantText by remember { mutableStateOf(transaction.merchant) }
    var selectedCat by remember { mutableStateOf<Category?>(categories.find { it.id == transaction.categoryId || it.name.equals(transaction.categoryName, ignoreCase = true) } ?: categories.firstOrNull()) }
    var txType by remember { mutableStateOf(transaction.type) }
    var descriptionText by remember { mutableStateOf(transaction.description) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(2.dp, TableBorderBlack)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Transaction",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )

                    Text(
                        text = "✕",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type Toggle (Debited vs Credited)
                Text(text = "Transaction Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextBlack)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Debit Chip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, if (txType == TransactionType.DEBIT) TableBorderBlack else Color.LightGray, RoundedCornerShape(8.dp))
                            .background(if (txType == TransactionType.DEBIT) TableBorderBlack else Color.White)
                            .clickable { txType = TransactionType.DEBIT }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Debited (Expense)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (txType == TransactionType.DEBIT) Color.White else TextBlack
                        )
                    }

                    // Credit Chip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, if (txType == TransactionType.CREDIT) Color(0xFF059669) else Color.LightGray, RoundedCornerShape(8.dp))
                            .background(if (txType == TransactionType.CREDIT) Color(0xFF059669) else Color.White)
                            .clickable { txType = TransactionType.CREDIT }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Credited (Income)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (txType == TransactionType.CREDIT) Color.White else TextBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("e.g. 500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TableBorderBlack,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Merchant / Party Input
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text(if (txType == TransactionType.CREDIT) "Received From (Sender)" else "Paid To (Merchant)") },
                    placeholder = { Text("e.g. Reliance Fresh, Ramesh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TableBorderBlack,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reason / Category Selector
                Text(text = "Reason / Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextBlack)
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = selectedCat?.id == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, if (isSel) TableBorderBlack else Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .background(if (isSel) TableBorderBlack else Color(0xFFF9FAFB))
                                .clickable { selectedCat = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${cat.iconKey} ${cat.name}",
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else TextBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional Note / Description
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add specific details...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TableBorderBlack,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Meta Info Box (Source App, Status)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Source: ${transaction.sourceApp.ifEmpty { "Bank Alert" }}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Status: ${transaction.status.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Save Changes and Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Delete Button
                    Box(
                        modifier = Modifier
                            .weight(0.38f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color(0xFFDC2626), RoundedCornerShape(8.dp))
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Delete", color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save Button
                    Box(
                        modifier = Modifier
                            .weight(0.62f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TableBorderBlack)
                            .clickable {
                                val amountVal = amountText.toDoubleOrNull()
                                if (amountVal != null && amountVal > 0 && merchantText.isNotBlank()) {
                                    val updated = transaction.copy(
                                        amount = amountVal,
                                        merchant = merchantText.trim(),
                                        categoryId = selectedCat?.id,
                                        categoryName = selectedCat?.name ?: "Others",
                                        type = txType,
                                        description = descriptionText.trim(),
                                        status = TransactionStatus.SAVED
                                    )
                                    onUpdate(updated)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Save Changes", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    categories: List<Category>,
    onAdd: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(categories.firstOrNull()) }
    var txType by remember { mutableStateOf(TransactionType.DEBIT) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, TableBorderBlack)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("e.g. 250") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Paid To / Received From") },
                    placeholder = { Text("e.g. Cafe Coffee Day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextBlack)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        val isSel = selectedCat?.id == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSel) TableBorderBlack else Color.LightGray, RoundedCornerShape(6.dp))
                                .background(if (isSel) TableBorderBlack else Color.White)
                                .clickable { selectedCat = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else TextBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Cancel", fontSize = 12.sp, color = TextBlack)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TableBorderBlack)
                            .clickable {
                                val amt = amountText.toDoubleOrNull()
                                if (amt != null && merchantText.isNotBlank()) {
                                    val newTx = Transaction(
                                        amount = amt,
                                        merchant = merchantText.trim(),
                                        categoryId = selectedCat?.id,
                                        categoryName = selectedCat?.name ?: "Others",
                                        type = txType,
                                        sourceApp = "Manual",
                                        status = TransactionStatus.SAVED
                                    )
                                    onAdd(newTx)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextBlack)
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toInt().toString()
    } else {
        String.format("%.2f", amount)
    }
}
