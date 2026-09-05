package com.swiftyprotein.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
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
    val focusManager = LocalFocusManager.current
    val isPreview = LocalInspectionMode.current
    val auth = if (isPreview) null else FirebaseAuth.getInstance()

    // Automatically deselect text fields / hide soft keyboard when entering screen
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    // rememberSaveable is used to preserve the result across recreation of the activity
	// for special events, like device display rotation
    val isBiometricAvailable = rememberSaveable {
        if (isPreview) false else checkIfBiometricsAvailable(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
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
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                user?.reload()?.addOnCompleteListener { _ ->
                                    if (user.isEmailVerified) {
                                        navController.navigate("ligand_list") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        auth.signOut()
                                        Toast.makeText(
                                            context,
                                            "Account is not confirmed. Please check your email and click the verification link.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
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

        TextButton(
	        onClick = {
                focusManager.clearFocus()
                navController.navigate("create_account")
            }
		) {
            Text("Don't have an account? Create a new one!")
        }

        Spacer(modifier = Modifier.height(16.dp))

	    if (isBiometricAvailable)
	    {
	        Button(
	            onClick = {
		            showBiometricPrompt(context as FragmentActivity) { success ->
			            if (success) {
				            navController.navigate("ligand_list") {
					            popUpTo("login") { inclusive = true }
				            }
			            } else {
				            Toast.makeText(context, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
			            }
		            }
	            },
	            modifier = Modifier.fillMaxWidth()
	        ) {
	            Text("Login with Biometrics")
	        }
	    }

	    if (isEmulator() || com.swiftyprotein.config.DebugConfig.ENABLE_DEBUG_BYPASS)
	    {
		    if (isBiometricAvailable) {
			    Spacer(modifier = Modifier.height(8.dp))
		    }
		    Button(
			    onClick = {
				    navController.navigate("ligand_list") {
					    popUpTo("login") { inclusive = true }
				    }
	            },
			    modifier = Modifier.fillMaxWidth()
			) {
				Text("Enter without biometrics")
		    }
	    }
	    // What is Firebase?
	    // Firebase is a Google platform that provides backend services like Authentication, Database, and more,
	    // so developers don't have to manage servers.
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
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}

fun checkIfBiometricsAvailable(context: Context): Boolean {
	return try {
		val biometricManager = BiometricManager.from(context)
		val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
		val status = biometricManager.canAuthenticate(authenticators)

		if (status == BiometricManager.BIOMETRIC_SUCCESS) {
			true
		} else {
			val errorMessage = when (status) {
				BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware found"
				BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable"
				BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Biometric not activated. Please enroll it in the device settings"
				else -> "Biometric error, please use email/password"
			}
			Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
			false
		}
	} catch (_: Throwable) {
		false
	}
}
