package com.swiftyprotein

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swiftyprotein.ui.screens.*
import com.swiftyprotein.ui.theme.SwiftyProteinTheme
import java.io.File

class MainActivity : FragmentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Clear cached .cif files automatically whenever a new app version/build is installed
        clearCifCacheIfUpdated(this)

        val auth = FirebaseAuth.getInstance()

        // Observe lifecycle to force login when the focus is lost
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // App went to background (closed, switched app, or phone blocked)
                    // We sign out from Firebase if a Firebase user session exists
                    if (auth.currentUser != null) {
                        auth.signOut()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    // App coming back to foreground
                    if (::navController.isInitialized) {
                        try {
                            val currentRoute = navController.currentDestination?.route
                            // Allow splash, login, create_account, and reset_password without forcing login redirect
                            if (currentRoute != null && currentRoute != "splash" && currentRoute != "login" && currentRoute != "create_account" && currentRoute != "reset_password") {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        } catch (_: Exception) {
                            // Safely catch navigation state exceptions on app foreground restart
                        }
                    }
                }
                else -> {}
            }
        })

        setContent {
            SwiftyProteinTheme {
                navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("create_account") { CreateAccountScreen(navController) }
        composable("reset_password") { ResetPasswordScreen(navController) }
        composable("ligand_list") { LigandListScreen(navController) }
        composable("ligand_detail/{ligand}") { backStackEntry ->
            val ligand = backStackEntry.arguments?.getString("ligand") ?: ""
            LigandDetailScreen(navController, ligand)
        }
    }
}

private fun clearCifCacheIfUpdated(context: Context) {
    try {
        val prefs = context.getSharedPreferences("swifty_protein_prefs", Context.MODE_PRIVATE)
        val currentVersionCode = BuildConfig.VERSION_CODE
        val lastVersionCode = prefs.getInt("last_version_code", -1)

        if (lastVersionCode != currentVersionCode) {
            // Delete all cached .cif files on app version update
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".cif")) file.delete()
            }
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".cif")) file.delete()
            }
            File(context.cacheDir, "images").deleteRecursively()

            prefs.edit().putInt("last_version_code", currentVersionCode).apply()
        }
    } catch (_: Exception) {
        // Safe fallback
    }
}
