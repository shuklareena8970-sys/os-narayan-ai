package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfile
import com.example.ui.NarayanViewModel

@Composable
fun AuthGate(
    viewModel: NarayanViewModel,
    content: @Composable (userId: String) -> Unit
) {
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()

    if (currentUserProfile == null) {
        InstagramAuthScreen(viewModel = viewModel)
    } else {
        content(currentUserProfile!!.username)
    }
}

@Composable
fun InstagramAuthScreen(viewModel: NarayanViewModel) {
    var isSignUpMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    // Login Fields
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Sign-up Fields (Instagram-style ID making)
    var fullNameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var showSignUpPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserProfile.ROLE_STUDENT) }
    var adminSecretKey by remember { mutableStateOf("") }
    var signUpError by remember { mutableStateOf<String?>(null) }

    // Live validation for Instagram username
    val usernameValidation = remember(usernameInput) {
        if (usernameInput.isBlank()) null
        else viewModel.validateInstagramUsername(usernameInput)
    }
    val isUsernameValid = usernameInput.isNotBlank() && usernameValidation == null

    // Password strength calculation
    val passwordStrength = remember(signUpPassword) {
        when {
            signUpPassword.length >= 8 && signUpPassword.any { it.isDigit() } && signUpPassword.any { !it.isLetterOrDigit() } -> 1.0f to ("Strong" to Color(0xFF2E7D32))
            signUpPassword.length >= 6 -> 0.65f to ("Good" to Color(0xFFF57C00))
            signUpPassword.isNotEmpty() -> 0.3f to ("Weak" to Color(0xFFD32F2F))
            else -> 0.0f to ("" to Color.Transparent)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Instagram-style Gradient Story Ring around Logo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF833AB4), // Purple
                                Color(0xFFFD1D1D), // Red
                                Color(0xFFFCB045)  // Orange
                            )
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.narayana_logo),
                        contentDescription = "Narayana Public School Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Narayana Public School",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "GONDA (U.P.) • LIVE LIFE FULLY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isSignUpMode) "Sign up with your custom study ID" else "Intelligent Study OS & Curated Lectures",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isSignUpMode) {
                // ==========================================
                // INSTAGRAM LOGIN FORM
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Log In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (loginError != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = loginError!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = loginIdentifier,
                            onValueChange = {
                                loginIdentifier = it
                                loginError = null
                            },
                            label = { Text("Username, phone, or email") },
                            placeholder = { Text("@username or superadmin") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = {
                                loginPassword = it
                                loginError = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                    Icon(
                                        imageVector = if (showLoginPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (loginIdentifier.isBlank() || loginPassword.isBlank()) {
                                    loginError = "Please enter both username and password."
                                    return@Button
                                }
                                val result = viewModel.loginWithInstagramId(loginIdentifier, loginPassword)
                                result.onFailure {
                                    loginError = it.message ?: "Login failed"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0095F6) // Instagram Blue
                            )
                        ) {
                            Text("Log In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "  OR  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google Sign-In with Firebase Authentication
                        OutlinedButton(
                            onClick = {
                                isGoogleSigningIn = true
                                loginError = null
                                com.example.data.firebase.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isGoogleSigningIn = false
                                        val fbUser = Firebase.auth.currentUser
                                        if (fbUser != null) {
                                            viewModel.loginWithGoogleFirebase(
                                                email = fbUser.email ?: "",
                                                displayName = fbUser.displayName,
                                                uid = fbUser.uid
                                            )
                                        }
                                    },
                                    onAuthError = { err ->
                                        isGoogleSigningIn = false
                                        loginError = err
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isGoogleSigningIn = false
                                    }
                                )
                            },
                            enabled = !isGoogleSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGoogleSigningIn) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign in with Google (Firebase)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Fill Demo Account Chips
                        Text(
                            text = "Quick One-Tap Logins:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = loginIdentifier == "superadmin",
                                onClick = {
                                    loginIdentifier = "superadmin"
                                    loginPassword = "admin123"
                                    loginError = null
                                },
                                label = { Text("👑 Super Admin (@superadmin)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = loginIdentifier == "student",
                                onClick = {
                                    loginIdentifier = "student"
                                    loginPassword = "study123"
                                    loginError = null
                                },
                                label = { Text("🎓 Student (@student)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.continueAsOfflineStudent() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("continue_guest_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Continue as Offline Student")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Instagram-style Switch to Sign Up Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Sign up",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0095F6),
                            modifier = Modifier
                                .clickable {
                                    isSignUpMode = true
                                    loginError = null
                                }
                                .testTag("switch_to_signup")
                        )
                    }
                }
            } else {
                // ==========================================
                // INSTAGRAM SIGN-UP / ID MAKING PROCESS
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Create Your Study ID",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pick your @username handle just like on Instagram.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (signUpError != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = signUpError!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Step 1: Full Name
                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = {
                                fullNameInput = it
                                signUpError = null
                            },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. Ajay Shukla") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_fullname_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Step 2: Instagram-style Username with Live Availability
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = {
                                usernameInput = it.lowercase().replace(" ", "_")
                                signUpError = null
                            },
                            label = { Text("Username Handle *") },
                            placeholder = { Text("ajay_shukla or narayan_educator") },
                            leadingIcon = {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                if (isUsernameValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Available",
                                        tint = Color(0xFF2E7D32)
                                    )
                                } else if (usernameValidation != null) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Invalid",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            isError = usernameValidation != null,
                            supportingText = {
                                if (isUsernameValid) {
                                    Text(
                                        text = "✓ @${usernameInput.removePrefix("@")} is available!",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (usernameValidation != null) {
                                    Text(text = usernameValidation, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Letters, numbers, '.', and '_' allowed (3-30 chars)")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Step 3: Password with strength meter
                        OutlinedTextField(
                            value = signUpPassword,
                            onValueChange = {
                                signUpPassword = it
                                signUpError = null
                            },
                            label = { Text("Password (min 6 chars) *") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showSignUpPassword = !showSignUpPassword }) {
                                    Icon(
                                        imageVector = if (showSignUpPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showSignUpPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (signUpPassword.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { passwordStrength.first },
                                    color = passwordStrength.second.second,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = passwordStrength.second.first,
                                    color = passwordStrength.second.second,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Step 4: Account Type (Student or Super Admin)
                        Text(
                            text = "Account Role:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedRole == UserProfile.ROLE_STUDENT,
                                onClick = { selectedRole = UserProfile.ROLE_STUDENT },
                                label = { Text("Student Learner") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedRole == UserProfile.ROLE_SUPER_ADMIN,
                                onClick = { selectedRole = UserProfile.ROLE_SUPER_ADMIN },
                                label = { Text("👑 Super Admin") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (selectedRole == UserProfile.ROLE_SUPER_ADMIN) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = adminSecretKey,
                                onValueChange = {
                                    adminSecretKey = it
                                    signUpError = null
                                },
                                label = { Text("Super Admin Authorization Key *") },
                                placeholder = { Text("Default key: Admin@123") },
                                leadingIcon = {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (fullNameInput.isBlank() || usernameInput.isBlank() || signUpPassword.isBlank()) {
                                    signUpError = "Please fill in all required fields."
                                    return@Button
                                }
                                if (usernameValidation != null) {
                                    signUpError = usernameValidation
                                    return@Button
                                }
                                if (signUpPassword.length < 6) {
                                    signUpError = "Password must be at least 6 characters."
                                    return@Button
                                }
                                if (selectedRole == UserProfile.ROLE_SUPER_ADMIN) {
                                    if (adminSecretKey != "admin123" && adminSecretKey != "Admin@123") {
                                        signUpError = "Incorrect Super Admin Authorization Key."
                                        return@Button
                                    }
                                }

                                val result = viewModel.registerInstagramId(
                                    fullName = fullNameInput,
                                    username = usernameInput,
                                    pass = signUpPassword,
                                    role = selectedRole
                                )
                                result.onFailure {
                                    signUpError = it.message ?: "Sign up failed"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("signup_submit_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0095F6) // Instagram Blue
                            )
                        ) {
                            Text("Sign Up & Enter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Instagram-style Switch to Log In Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Have an account? ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Log in",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0095F6),
                            modifier = Modifier
                                .clickable {
                                    isSignUpMode = false
                                    signUpError = null
                                }
                                .testTag("switch_to_login")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
