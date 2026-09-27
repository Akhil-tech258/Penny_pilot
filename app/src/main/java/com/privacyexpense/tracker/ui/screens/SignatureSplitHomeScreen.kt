package com.privacyexpense.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyexpense.tracker.ui.theme.AvatarPeach
import com.privacyexpense.tracker.ui.theme.GlowAmber
import com.privacyexpense.tracker.ui.theme.TableBorderBlack
import com.privacyexpense.tracker.ui.theme.TextBlack
import com.privacyexpense.tracker.ui.theme.TextSecondary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun SignatureSplitHomeScreen(
    userName: String = "Akhil",
    pendingCount: Int = 1,
    onNavigateToTransactions: () -> Unit,
    onNavigateToRecurringBills: () -> Unit = {},
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var isProfileMaximized by remember { mutableStateOf(false) }
    var activeDualSection by remember { mutableStateOf<String?>(null) }

    // Intercept back button if profile is maximized
    BackHandler(enabled = isProfileMaximized) {
        isProfileMaximized = false
    }

    // Apple-style spring animation specs
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val avatarSize by animateDpAsState(
        targetValue = if (isProfileMaximized) 100.dp else 84.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "avatarSize"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Subtle top-left warm ambient glow matching user's Canva design
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GlowAmber.copy(alpha = 0.55f),
                            GlowAmber.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Vertical Split Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // LEFT PANEL: Lowercase italic navigation items
            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // "profile" item
                NavItem(
                    text = "profile",
                    isSelected = isProfileMaximized,
                    onClick = { isProfileMaximized = true }
                )

                Spacer(modifier = Modifier.height(36.dp))

                // "transactions" item with Apple dual trigger
                NavItem(
                    text = "transactions",
                    badgeCount = pendingCount,
                    isSelected = activeDualSection == "transactions",
                    onClick = {
                        activeDualSection = "transactions"
                        onNavigateToTransactions()
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // "recurring bills" item
                NavItem(
                    text = "recurring bills",
                    isSelected = activeDualSection == "recurring_bills",
                    onClick = {
                        activeDualSection = "recurring_bills"
                        onNavigateToRecurringBills()
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // "backup" item
                NavItem(
                    text = "backup",
                    isSelected = activeDualSection == "backup",
                    onClick = {
                        activeDualSection = "backup"
                        onNavigateToBackup()
                    }
                )

                Spacer(modifier = Modifier.height(36.dp))

                // "settings" item
                NavItem(
                    text = "settings",
                    isSelected = activeDualSection == "settings",
                    onClick = {
                        activeDualSection = "settings"
                        onNavigateToSettings()
                    }
                )
            }

            // VERTICAL DIVIDING LINE (Crisp 1.5dp black line from user's design)
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight(0.85f)
                    .align(Alignment.CenterVertically)
                    .background(TableBorderBlack.copy(alpha = 0.85f))
            )

            // RIGHT PANEL: Peach Avatar + "Hi Akhil" (Minimized Bar)
            Column(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
                    .padding(start = 8.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular Avatar (#F4D3A1 peach circle)
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(AvatarPeach)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isProfileMaximized = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Hi Akhil" in bold centered underneath
                Text(
                    text = "Hi $userName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tap to expand",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // FULLSCREEN MAXIMIZED PROFILE OVERLAY (Clean White/Warm Aesthetic)
        AnimatedVisibility(
            visible = isProfileMaximized,
            enter = fadeIn(springSpec) + slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 },
            exit = fadeOut(springSpec) + slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 }
        ) {
            MaximizedProfileView(
                userName = userName,
                onClose = { isProfileMaximized = false },
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun NavItem(
    text: String,
    badgeCount: Int = 0,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) TextBlack else TextBlack.copy(alpha = 0.85f),
            letterSpacing = 0.5.sp
        )

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5B869))
            )
        }
    }
}

@Composable
private fun MaximizedProfileView(
    userName: String,
    onClose: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.LightGray, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", fontSize = 13.sp, color = TextBlack)
            }
        }

        // Expanded Profile Details in White/Warm Aesthetic
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large peach avatar with subtle ring
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AvatarPeach)
                    .border(3.dp, Color(0xFFE5B869), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Local Profile",
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // White Card with information (Matching Screen 6/7)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, TableBorderBlack, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Hi, $userName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    Text(
                        text = "Good to see you!",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Name", fontSize = 13.sp, color = TextSecondary)
                        Text(text = userName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Account", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "Local Encrypted Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Storage", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "Offline • 100% Private", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF10B981))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Log Out Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TableBorderBlack)
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log Out",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
