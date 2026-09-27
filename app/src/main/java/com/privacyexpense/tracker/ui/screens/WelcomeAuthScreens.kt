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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.privacyexpense.tracker.R
import com.privacyexpense.tracker.ui.theme.AccentGold
import com.privacyexpense.tracker.ui.theme.AvatarPeach
import com.privacyexpense.tracker.ui.theme.GlowAmber
import com.privacyexpense.tracker.ui.theme.TableBorderBlack
import com.privacyexpense.tracker.ui.theme.TextBlack
import com.privacyexpense.tracker.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onCreateProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRestoreBackupClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Ambient warm glow
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Emblem & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 50.dp)
            ) {
                // Penny Pilot emblem
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA6A6A6))
                        .border(2.dp, TableBorderBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_penny_pilot_splash),
                        contentDescription = "Penny Pilot Emblem",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Penny Pilot",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Personal Finance Co-Pilot",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // Buttons Group (White/Warm Aesthetic)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Create Profile (Warm Gold Accent button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentGold)
                        .clickable(onClick = onCreateProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Profile",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                }

                // Login (Crisp Bordered button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, TableBorderBlack, RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable(onClick = onLoginClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Login",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextBlack
                    )
                }

                // Restore Backup (Subtle Bordered button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable(onClick = onRestoreBackupClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Restore Backup",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "Local • Secure • Private",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun CreateProfileScreen(
    onCreateProfile: (name: String, password: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                .padding(24.dp)
        ) {
            Text(
                text = "← Back",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Create Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Set up your local profile to get started",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Enter your name (e.g. Akhil)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TableBorderBlack,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = TableBorderBlack
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Enter local password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TableBorderBlack,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = TableBorderBlack
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                placeholder = { Text("Re-enter password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TableBorderBlack,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = TableBorderBlack
                )
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = msg, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TableBorderBlack)
                    .clickable {
                        if (name.isBlank()) {
                            errorMessage = "Please enter your name"
                        } else if (password.length < 4) {
                            errorMessage = "Password must be at least 4 characters"
                        } else if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                        } else {
                            onCreateProfile(name.trim(), password)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your data will be stored locally and encrypted.",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LoginScreen(
    profileName: String,
    onLogin: (password: String) -> Boolean,
    onBack: () -> Unit,
    onResetProfileClick: () -> Unit = {}
) {
    BackHandler { onBack() }

    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                .padding(24.dp)
        ) {
            Text(
                text = "← Back",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Welcome Back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Please login to continue",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Profile Badge Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .background(Color(0xFFFAFAFA))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AvatarPeach),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileName.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(text = profileName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    Text(text = "Local Profile", fontSize = 11.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TableBorderBlack,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = TableBorderBlack
                )
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = msg, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TableBorderBlack)
                    .clickable {
                        val success = onLogin(password)
                        if (!success) {
                            errorMessage = "Incorrect password"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Login",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onResetProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Switch or Reset Profile",
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
