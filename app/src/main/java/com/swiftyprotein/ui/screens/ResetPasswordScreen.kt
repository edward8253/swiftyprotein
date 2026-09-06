package com.swiftyprotein.ui.screens

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isPreview = LocalInspectionMode.current
    val auth = if (isPreview) null else FirebaseAuth.getInstance()

    // Automatically deselect text fields / hide soft keyboard when entering screen
    LaunchedEffect(Unit) {
        delay(100)
        focusManager.clearFocus(force = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus(force = true)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Login")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus(force = true)
                    })
                }
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Password Recovery", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            // Email / Username Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Username / Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Send Password Reset Link Button
            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isEmpty()) {
                        Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (auth == null) {
                        Toast.makeText(context, "Authentication service unavailable", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    @Suppress("DEPRECATION")
                    auth.fetchSignInMethodsForEmail(trimmedEmail).addOnCompleteListener { fetchTask ->
                        if (fetchTask.isSuccessful) {
                            val methods = fetchTask.result?.signInMethods
                            if (!methods.isNullOrEmpty()) {
                                auth.sendPasswordResetEmail(trimmedEmail)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            focusManager.clearFocus(force = true)
                                            Toast.makeText(
                                                context,
                                                "Password reset link sent to $trimmedEmail. Please check your email inbox to reset your password.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Error sending reset email: ${task.exception?.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            } else {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "No registered user found with email $trimmedEmail. Please check your email or register.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            // Fallback if fetchSignInMethodsForEmail fails or is restricted by Firebase Console
                            auth.sendPasswordResetEmail(trimmedEmail)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        focusManager.clearFocus(force = true)
                                        Toast.makeText(
                                            context,
                                            "Password reset link sent to $trimmedEmail. Please check your email inbox.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No registered user found for $trimmedEmail: ${task.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send Password Reset Link")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Login Button
            TextButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    navController.popBackStack()
                }
            ) {
                Text("Back to Login")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResetPasswordScreenPreview() {
    ResetPasswordScreen(rememberNavController())
}
