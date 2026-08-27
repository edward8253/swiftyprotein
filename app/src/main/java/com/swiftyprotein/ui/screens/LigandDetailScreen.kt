package com.swiftyprotein.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.swiftyprotein.data.Atom
import com.swiftyprotein.data.CifParser
import com.swiftyprotein.ui.opengl.MoleculeGlRenderer
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LigandDetailScreen(navController: NavHostController, ligand: String) {
    val context = LocalContext.current
    val renderer = remember { MoleculeGlRenderer(context) }
    var selectedAtom by remember { mutableStateOf<Atom?>(null) }
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }

    // Load CIF data
    LaunchedEffect(ligand) {
        try {
            val file = File(context.filesDir, "$ligand.cif")
            if (file.exists()) {
                val content = file.readText()
                val (atoms, bonds) = CifParser().parse(content)
                renderer.atoms = atoms
                renderer.bonds = bonds
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ligand) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        renderer.screenshotCallback = { bitmap ->
                            shareScreenshot(context, bitmap, ligand)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(2)
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        viewWidth = it.size.width
                        viewHeight = it.size.height
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            renderer.rotationY += pan.x / 5f
                            renderer.rotationX += pan.y / 5f
                            renderer.zoom *= zoom
                            if (renderer.zoom > -2f) renderer.zoom = -2f
                            if (renderer.zoom < -100f) renderer.zoom = -100f
                        }
                    }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val atom = renderer.pickAtom(offset.x, offset.y, viewWidth, viewHeight)
                                selectedAtom = atom
                            }
                        )
                    }
            )

            // Tooltip
            selectedAtom?.let { atom ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Element: ${atom.element}", color = Color.White)
                        Text("Name: ${atom.name}", color = Color.White)
                        Text("Residue: ${atom.residueName}", color = Color.White)
                        Text("Coords: (${atom.x}, ${atom.y}, ${atom.z})", color = Color.White)
                        Button(
                            onClick = { selectedAtom = null },
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

fun shareScreenshot(context: android.content.Context, bitmap: Bitmap, ligand: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "$ligand.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Molecule Screenshot"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
