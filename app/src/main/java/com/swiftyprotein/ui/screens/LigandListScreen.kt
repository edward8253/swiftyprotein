package com.swiftyprotein.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.swiftyprotein.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun LigandListScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var ligands by remember { mutableStateOf(listOf<String>()) }
    var isDownloading by remember { mutableStateOf(false) }
    val isPreview = LocalInspectionMode.current

    // Load ligands once
    LaunchedEffect(Unit) {
        if (isPreview) {
            ligands = listOf("HEM", "ATP", "GLA", "GLC")
        } else {
            ligands = loadLigands(context)
        }
    }

    val filteredLigands = ligands.filter { it.contains(searchQuery, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                                        navController.navigate("confirmation/$ligand")
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

fun loadLigands(context: Context): List<String> {
    return try {
        context.resources.openRawResource(R.raw.ligands)
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun downloadCif(context: Context, ligand: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://files.rcsb.org/ligands/view/$ligand.cif")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // For this example, we just read the content to simulate a download
                connection.inputStream.bufferedReader().use { it.readText() }
                true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                }
                false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LigandListScreenPreview() {
    LigandListScreen(rememberNavController())
}
