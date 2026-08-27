package com.swiftyprotein.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val auth = if (isPreview) null else FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "SwiftyProtein Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (auth == null) return@Button
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                navController.navigate("ligand_list") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login with Firebase")
        }

        TextButton(onClick = {
            if (auth == null) return@TextButton
            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Account created! You can now login.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Creation failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Please fill in email and password to create an account", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Create Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
	            navController.navigate("ligand_list") {
		            popUpTo("login") { inclusive = true }
	            }
//	            if (isEmulator()) {
//                } else {
//                    showBiometricPrompt(context as FragmentActivity) { success ->
//                        if (success) {
//                            navController.navigate("ligand_list") {
//                                popUpTo("login") { inclusive = true }
//                            }
//                        } else {
//                            Toast.makeText(context, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login with Biometrics")
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "What is Firebase?\nFirebase is a Google platform that provides backend services like Authentication, Database, and more, so developers don't have to manage servers.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

fun isEmulator(): Boolean {
    return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
            || android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || android.os.Build.PRODUCT.contains("sdk_google")
            || android.os.Build.PRODUCT.contains("google_sdk")
            || android.os.Build.PRODUCT.contains("sdk")
            || android.os.Build.PRODUCT.contains("sdk_x86")
            || android.os.Build.PRODUCT.contains("vbox86p")
            || android.os.Build.PRODUCT.contains("emulator")
            || android.os.Build.PRODUCT.contains("simulator")
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(rememberNavController())
}

fun showBiometricPrompt(
    activity: FragmentActivity,
    onResult: (Boolean) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Graceful handling as requested: just return false, Toast is shown in the composable
                onResult(false)
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Login")
        .setSubtitle("Log in using your biometric credential")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}
