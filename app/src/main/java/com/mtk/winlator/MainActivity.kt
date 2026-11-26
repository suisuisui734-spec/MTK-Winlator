package com.mtk.winlator

import android.opengl.GLES20
import android.os.Build
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var pickedUri by mutableStateOf<String?>(null)
    private val exePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pickedUri = it.toString()
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MTKApp(pickedUri) { exePicker.launch(arrayOf("*/*")) } }
    }
}

@Composable
private fun MTKApp(selectedUri: String?, pickExe: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val scheme = darkColorScheme(primary = Color(0xFF68DFA3), secondary = Color(0xFF87C8FF),
        background = Color(0xFF0D1115), surface = Color(0xFF151C22))
    MaterialTheme(colorScheme = scheme) {
        Scaffold(containerColor = scheme.background, bottomBar = {
            NavigationBar(containerColor = scheme.surface) {
                NavigationBarItem(page == 0, { page = 0 }, icon = { Icon(Icons.Default.SportsEsports, null) }, label = { Text("Games") })
                NavigationBarItem(page == 1, { page = 1 }, icon = { Icon(Icons.Default.Info, null) }, label = { Text("Device") })
                NavigationBarItem(page == 2, { page = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }) { inset ->
            when(page) {
                0 -> GamesPage(selectedUri, pickExe, Modifier.padding(inset))
                1 -> DevicePage(Modifier.padding(inset))
                else -> SettingsPage(Modifier.padding(inset))
            }
        }
    }
}

@Composable private fun AppHeader(sub: String) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text("MTK WINLATOR", fontSize = 25.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun GamesPage(uri: String?, pickExe: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppHeader("MediaTek Mali Edition")
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Runtime status", fontWeight = FontWeight.Bold)
                Text("Not installed", color = MaterialTheme.colorScheme.error)
                Text("Wine / Box64 and graphics runtime are not included in this foundation build.", fontSize = 13.sp)
                Button(onClick = {}, enabled = false) { Text("Runtime installer — planned") }
            }
        }
        Button(onClick = pickExe, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Select Windows EXE")
        }
        if (uri != null) Card {
            Column(Modifier.padding(14.dp)) {
                Text("Selected URI", fontWeight = FontWeight.Bold)
                Text(uri, fontSize = 12.sp)
                Text("Selection only; execution is not available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Game library", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card { Column(Modifier.padding(16.dp)) {
            Text("No entries saved yet.")
            Text("Add an executable file to select it. Runtime execution is not implemented.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
}

@Composable private fun DevicePage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val glRenderer = remember { getRendererInfo() ?: "Not detected" }
    val rows = listOf(
        "Manufacturer" to Build.MANUFACTURER,
        "Model" to Build.MODEL,
        "Device codename" to Build.DEVICE,
        "Android version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Supported ABI" to Build.SUPPORTED_ABIS.joinToString(),
        "CPU cores" to Runtime.getRuntime().availableProcessors().toString(),
        "App heap limit" to "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB",
        "OpenGL renderer" to glRenderer,
        "Vulkan feature" to if (context.packageManager.hasSystemFeature("android.hardware.vulkan.version")) "Feature advertised" else "Not advertised"
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppHeader("Hardware diagnostics")
        rows.forEach { (key, value) -> Card { Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(key, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Medium)
        } } }
        Text("Vulkan reporting is only a feature hint; detailed driver capability tests are not implemented.", fontSize = 12.sp)
    }
}

private fun getRendererInfo(): String? = runCatching {
    val view = android.opengl.GLSurfaceView(null)
    view.setRenderer(object : android.opengl.GLSurfaceView.Renderer {
        override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {}
        override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {}
        override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {}
    })
    // Renderer query needs a GL context; this placeholder intentionally returns a safe unavailable value.
    "GL renderer query pending"
}.getOrNull()

@Composable private fun SettingsPage(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppHeader("Settings")
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Performance profiles", fontWeight = FontWeight.Bold)
            Text("Lite • Balanced • Performance")
            Text("Presets are UI-only placeholders pending runtime integration.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Project information", fontWeight = FontWeight.Bold)
            Text("Launcher foundation v0.1.0")
            Text("Independent project; not an official Winlator build.")
        } }
    }
}
