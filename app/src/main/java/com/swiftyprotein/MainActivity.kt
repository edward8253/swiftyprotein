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

        // Observe lifecycle to force login on foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (::navController.isInitialized) {
                    val currentRoute = navController.currentDestination?.route
                    // If we are not on splash or login, we force the user to login again
                    if (currentRoute != null && currentRoute != "splash" && currentRoute != "login") {
                        navController.navigate("login") {
                            // Clear backstack to prevent going back to protected screens
                            popUpTo(0)
                        }
                    }
                }
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
        composable("confirmation/{ligand}") { backStackEntry ->
            val ligand = backStackEntry.arguments?.getString("ligand") ?: ""
            ConfirmationScreen(navController, ligand)
        }
    }
}
