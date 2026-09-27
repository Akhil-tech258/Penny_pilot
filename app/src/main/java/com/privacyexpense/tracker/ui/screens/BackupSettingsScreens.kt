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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyexpense.tracker.data.model.Category
import com.privacyexpense.tracker.ui.theme.AccentGold
import com.privacyexpense.tracker.ui.theme.AvatarPeach
import com.privacyexpense.tracker.ui.theme.TableBorderBlack
import com.privacyexpense.tracker.ui.theme.TextBlack
import com.privacyexpense.tracker.ui.theme.TextSecondary

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onCreateBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onClearAllDataClick: () -> Unit = {},
    statusMessage: String? = null
) {
    BackHandler { onBack() }
    var showClearDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "← Home",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Encrypted Backup",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Zero-cloud local file export & restore",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Create Backup Tile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, TableBorderBlack, RoundedCornerShape(14.dp))
                    .clickable { onCreateBackupClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AvatarPeach.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💾", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Create Encrypted Backup", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        Text(text = "Export AES-256 encrypted JSON to phone storage", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Restore Backup Tile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .clickable { onRestoreBackupClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📂", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Restore from Backup File", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        Text(text = "Select a previously saved backup file to restore", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clear All Data Tile (Wipes all transaction history)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp))
                    .clickable { showClearDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🗑️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Clear All Transactions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text(text = "Delete old records (e.g. ₹650 Reliance Fresh) from local database", fontSize = 11.sp, color = Color(0xFFB91C1C))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Checklist Card (Storyboard Screen 12)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Encrypted Backup Includes:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "✓ All transaction history (amounts, merchants, dates)", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "✓ Custom categories and notification priority", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "✓ User profile credentials and settings", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "✓ AES-256-GCM authenticated cipher with PBKDF2 salt", fontSize = 12.sp, color = TextSecondary)
                }
            }

            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Text(text = msg, fontSize = 12.sp, color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "🔒 Your data never leaves this device unless you export it.",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Transactions?") },
            text = { Text("Are you sure you want to delete all recorded transactions from the local database? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAllDataClick()
                    showClearDialog = false
                }) {
                    Text("Clear All", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextBlack)
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    categories: List<Category>,
    onAddCategory: (name: String, icon: String) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onToggleCategoryNotification: (Long, Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var detectionEnabled by remember { mutableStateOf(true) }
    var appLockEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatIcon by remember { mutableStateOf("🏷️") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "← Home",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Permissions, categorization rules & alerts",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. System Permissions Access
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "System Permissions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Required for real-time PhonePe, Google Pay & Paytm listening", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, TableBorderBlack, RoundedCornerShape(8.dp))
                                .clickable { onOpenNotificationSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Notification Access", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AvatarPeach)
                                .clickable { onRequestNotificationPermission() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Allow Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Category Manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Category Manager", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    Text(text = "Top categories appear in notification 1-tap buttons", fontSize = 11.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGold)
                        .clickable { showAddCategoryDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                            .background(Color(0xFFFAFAFA))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AvatarPeach.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = cat.iconKey.take(2), fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = cat.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextBlack)
                                Text(
                                    text = if (cat.isNotificationEnabled) "Shown in notification" else "Hidden from notification",
                                    fontSize = 10.sp,
                                    color = if (cat.isNotificationEnabled) Color(0xFF10B981) else TextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = cat.isNotificationEnabled,
                                onCheckedChange = { onToggleCategoryNotification(cat.id, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TableBorderBlack, checkedTrackColor = AvatarPeach)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✕",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .clickable { onDeleteCategory(cat.id) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. App Preferences Toggles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Transaction Detection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        Text(text = "Listen for bank SMS & UPI notifications", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = detectionEnabled,
                        onCheckedChange = { detectionEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TableBorderBlack, checkedTrackColor = AvatarPeach)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Notification Vibration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        Text(text = "Vibrate on transaction detection", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TableBorderBlack, checkedTrackColor = AvatarPeach)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "App Lock on Resume", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        Text(text = "Require password when reopening app", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { appLockEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TableBorderBlack, checkedTrackColor = AvatarPeach)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Penny Pilot V1.0 • Privacy-First Expense Flight",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Investment, Fuel, Pets") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCatIcon,
                        onValueChange = { newCatIcon = it },
                        label = { Text("Icon Emoji") },
                        placeholder = { Text("e.g. ⛽, 📈, 🐶") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCatName.isNotBlank()) {
                        onAddCategory(newCatName.trim(), newCatIcon.trim().ifEmpty { "🏷️" })
                        newCatName = ""
                        newCatIcon = "🏷️"
                        showAddCategoryDialog = false
                    }
                }) {
                    Text("Add", color = TableBorderBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
