package com.swiftyprotein

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

class MainActivity : FragmentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe lifecycle to force login on focus loss
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // App went to background (closed, switched app, or phone blocked)
                    // We sign out from Firebase to ensure the session is cleared
                    FirebaseAuth.getInstance().signOut()
                }
                Lifecycle.Event.ON_START -> {
                    // App coming back to foreground
                    if (::navController.isInitialized) {
                        val currentRoute = navController.currentDestination?.route
                        // If we were on a protected screen, force return to login
                        if (currentRoute != null && currentRoute != "splash" && currentRoute != "login") {
                            navController.navigate("login") {
                                // Clear backstack to prevent going back
                                popUpTo(0)
                            }
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
        composable("ligand_list") { LigandListScreen(navController) }
        composable("ligand_detail/{ligand}") { backStackEntry ->
            val ligand = backStackEntry.arguments?.getString("ligand") ?: ""
            LigandDetailScreen(navController, ligand)
        }
    }
}
