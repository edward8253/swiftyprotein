package com.swiftyprotein.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.swiftyprotein.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LigandListScreen(navController: NavHostController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var ligands by remember { mutableStateOf(listOf<String>()) }
    var isDownloading by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val isPreview = LocalInspectionMode.current

    // Pressing back on LigandList logs out and returns to the login screen
    BackHandler {
        if (!isPreview) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}
        }
        focusManager.clearFocus(force = true)
        // keyboardController?.hide()
        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }
    }

    // Clear any active focus so the search label / soft keyboard does not activate automatically
    LaunchedEffect(Unit) {
        delay(200.milliseconds)
        focusManager.clearFocus(force = true)
	    keyboardController?.hide()
	    ligands = if (isPreview) {
		    listOf("HEM", "ATP", "GLA", "GLC")
	    } else {
		    loadLigands(context)
	    }
    }

    val filteredLigands = ligands.filter { it.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ligands") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                menuExpanded = false
                                focusManager.clearFocus()
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo(0)
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    }
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Ligands") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredLigands) { ligand ->
                        ListItem(
                            headlineContent = { Text(ligand) },
                            modifier = Modifier.clickable {
                                if (!isDownloading) {
                                    isDownloading = true
                                    scope.launch {
                                        val success = downloadCif(context, ligand)
                                        isDownloading = false
                                        if (success) {
                                            navController.navigate("ligand_detail/$ligand")
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

fun loadLigands(context: Context): List<String> {
    return try {
        context.resources.openRawResource(R.raw.ligands)
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun downloadCif(context: Context, ligand: String): Boolean {
    return withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "$ligand.cif")
        val legacyFile = File(context.filesDir, "$ligand.cif")

        try {
            val url = URL("https://files.rcsb.org/ligands/view/$ligand.cif")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().use { it.readText() }
                // Store in temporary cache folder
                cacheFile.writeText(content)
                true
            } else {
                val localFile = if (cacheFile.exists() && cacheFile.length() > 0) cacheFile
                else if (legacyFile.exists() && legacyFile.length() > 0) legacyFile
                else null

                if (localFile != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Server error. Loaded $ligand from cache.", Toast.LENGTH_SHORT).show()
                    }
                    true
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Download failed: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                    false
                }
            }
        } catch (_: Exception) {
            val localFile = if (cacheFile.exists() && cacheFile.length() > 0) cacheFile
            else if (legacyFile.exists() && legacyFile.length() > 0) legacyFile
            else null

            if (localFile != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Offline mode: Loaded $ligand from cache", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Offline and no cached file found for $ligand", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LigandListScreenPreview() {
    LigandListScreen(rememberNavController())
}
