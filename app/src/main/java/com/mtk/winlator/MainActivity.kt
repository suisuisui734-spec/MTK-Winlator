package com.mtk.winlator

import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.opengl.GLES20
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.mtk.winlator.diagnostics.DeviceDiagnostics
import com.mtk.winlator.runtime.AndroidGraphicsCapabilities
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import com.mtk.winlator.library.GameEntry
import com.mtk.winlator.library.GameContainerConfigSynchronizer
import com.mtk.winlator.library.isWindowsExecutableName
import com.mtk.winlator.runtime.RuntimeContainerRegistryFactory
import com.mtk.winlator.runtime.RuntimeComponentInventory
import com.mtk.winlator.runtime.RuntimeInventoryState
import com.mtk.winlator.runtime.RuntimeReadinessEvidence
import com.mtk.winlator.runtime.RuntimeReadinessEvaluator
import java.util.concurrent.Executors


private fun formatFileSize(sizeBytes: Long?): String {
    if (sizeBytes == null || sizeBytes < 0L) return "Unknown"
    if (sizeBytes < 1024L) return "$sizeBytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = sizeBytes.toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return "%.1f %s".format(value, units[unitIndex])
}

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("mtk_winlator", MODE_PRIVATE) }
    private var games by mutableStateOf<List<GameEntry>>(emptyList())
    private var profile by mutableStateOf("Balanced")
    private val containerSyncExecutor = Executors.newSingleThreadExecutor()

    private fun synchronizeContainerMetadata(entries: List<GameEntry>) {
        val snapshot = entries.toList()
        containerSyncExecutor.execute {
            val result = runCatching {
                GameContainerConfigSynchronizer(
                    RuntimeContainerRegistryFactory.create(applicationContext)
                ).synchronize(snapshot)
            }
            if (result.isFailure || result.getOrNull()?.successful == false) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Container preferences could not be fully synchronized.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private val exePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val title = queryDisplayName(it) ?: it.lastPathSegment ?: "Selected file"
            if (!isWindowsExecutableName(title)) {
                Toast.makeText(this, "Please select a Windows .exe file.", Toast.LENGTH_LONG).show()
                return@let
            }
            runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val uriText = it.toString()
            if (games.any { game -> game.executableUri == uriText }) {
                Toast.makeText(this, "This file is already in your library.", Toast.LENGTH_SHORT).show()
            } else {
                games = games + GameEntry(uriText, title, uriText, fileSizeBytes = queryFileSize(it))
                saveGames(games)
                Toast.makeText(this, "Added to library. Runtime launching is not available yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun normalizeGraphicsBackend(value: String): String =
        value.uppercase().takeIf { it in setOf("AUTO", "OPENGL", "VULKAN") } ?: "AUTO"

    private fun normalizeResolution(width: Int, height: Int): Pair<Int, Int> =
        if ((width to height) in listOf(960 to 540, 1280 to 720, 1600 to 900)) width to height else 1280 to 720

    private fun normalizeMemoryLimit(value: Int): Int = value.takeIf { it in setOf(50, 75, 90) } ?: 75

    private fun saveGames(entries: List<GameEntry>) {
        val array = JSONArray()
        entries.forEach { entry -> array.put(JSONObject().put("id", entry.id).put("title", entry.title).put("uri", entry.executableUri).put("profile", entry.profileId).put("size", entry.fileSizeBytes ?: -1L).put("graphics_backend", normalizeGraphicsBackend(entry.graphicsBackend)).put("resolution_width", normalizeResolution(entry.resolutionWidth, entry.resolutionHeight).first).put("resolution_height", normalizeResolution(entry.resolutionWidth, entry.resolutionHeight).second).put("memory_limit_percent", normalizeMemoryLimit(entry.memoryLimitPercent))) }
        prefs.edit().putString("games_json", array.toString()).apply()
        synchronizeContainerMetadata(entries)
    }
    private fun loadGames(): List<GameEntry> = runCatching {
        val stored = prefs.getString("games_json", null)
        if (stored != null) { val array = JSONArray(stored); List(array.length()) { index -> array.getJSONObject(index).let { GameEntry(it.optString("id"), it.optString("title"), it.optString("uri"), it.optString("profile", "balanced"), it.optLong("size", -1L).takeIf { size -> size >= 0L }, normalizeGraphicsBackend(it.optString("graphics_backend", "AUTO")), normalizeResolution(it.optInt("resolution_width", 1280), it.optInt("resolution_height", 720)).first, normalizeResolution(it.optInt("resolution_width", 1280), it.optInt("resolution_height", 720)).second, normalizeMemoryLimit(it.optInt("memory_limit_percent", 75))) } } }
        else { val oldUri = prefs.getString("exe_uri", null); if (oldUri != null) listOf(GameEntry(oldUri, prefs.getString("exe_name", "Selected executable") ?: "Selected executable", oldUri)) else emptyList() }
    }.getOrElse { emptyList() }
    private fun queryFileSize(uri: Uri): Long? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0).takeIf { it >= 0L } else null
        }
    }.getOrNull()


    private fun queryDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        games = loadGames()
        if (prefs.getString("games_json", null) == null && games.isNotEmpty()) saveGames(games)
        else synchronizeContainerMetadata(games)
        profile = prefs.getString("profile", "Balanced") ?: "Balanced"
        setContent {
            MTKApp(games, profile,
                onPickExe = { exePicker.launch(arrayOf("*/*")) },
                onRemove = { entry -> games = games.filterNot { it.id == entry.id }; saveGames(games) },
                onGameProfile = { entry, p -> games = games.map { if (it.id == entry.id) it.copy(profileId = p.lowercase()) else it }; saveGames(games) },
                onGameGraphicsBackend = { entry, backend -> games = games.map { if (it.id == entry.id) it.copy(graphicsBackend = normalizeGraphicsBackend(backend)) else it }; saveGames(games) },
                onGameResolution = { entry, width, height -> games = games.map { if (it.id == entry.id) it.copy(resolutionWidth = width, resolutionHeight = height) else it }; saveGames(games) },
                onGameMemoryLimit = { entry, percent -> games = games.map { if (it.id == entry.id) it.copy(memoryLimitPercent = normalizeMemoryLimit(percent)) else it }; saveGames(games) },
                onProfile = { p -> profile = p; prefs.edit().putString("profile", p).apply() })
        }
    }
}

@Composable
private fun MTKApp(games: List<GameEntry>, profile: String, onPickExe: () -> Unit, onRemove: (GameEntry) -> Unit, onGameProfile: (GameEntry, String) -> Unit, onGameGraphicsBackend: (GameEntry, String) -> Unit, onGameResolution: (GameEntry, Int, Int) -> Unit, onGameMemoryLimit: (GameEntry, Int) -> Unit, onProfile: (String) -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val scheme = darkColorScheme(primary = Color(0xFF68DFA3), secondary = Color(0xFF87C8FF), background = Color(0xFF0D1115), surface = Color(0xFF151C22))
    MaterialTheme(colorScheme = scheme) {
        Scaffold(containerColor = scheme.background, bottomBar = {
            NavigationBar(containerColor = scheme.surface) {
                NavigationBarItem(page == 0, { page = 0 }, icon = { Icon(Icons.Default.SportsEsports, null) }, label = { Text("Games") })
                NavigationBarItem(page == 1, { page = 1 }, icon = { Icon(Icons.Default.Info, null) }, label = { Text("Device") })
                NavigationBarItem(page == 2, { page = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }) { inset ->
            when (page) {
                0 -> GamesPage(games, onPickExe, onRemove, onGameProfile, onGameGraphicsBackend, onGameResolution, onGameMemoryLimit, Modifier.padding(inset))
                1 -> DevicePage(Modifier.padding(inset))
                else -> SettingsPage(profile, onProfile, Modifier.padding(inset))
            }
        }
    }
}

@Composable private fun RuntimeReadinessCard() {
    val evidence = remember {
        RuntimeReadinessEvidence(
            arm64DeviceSupported = Build.SUPPORTED_64_BIT_ABIS.any { it == "arm64-v8a" },
            arm64ProcessSupported = android.os.Process.is64Bit(),
            // These remain false until a production key and verified packages are supplied.
            productionManifestKeyConfigured = false,
            signedRuntimeManifestAvailable = false,
            runtimePackageInstalled = false,
            wineAvailable = false,
            box64Available = false,
            graphicsBackendAvailable = false
        )
    }
    val report = remember(evidence) { RuntimeReadinessEvaluator.evaluate(evidence) }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Runtime readiness", fontWeight = FontWeight.Bold)
            Text(
                if (report.isReadyForWindowsExecution) "Ready for execution"
                else "${report.readyCount}/${report.totalCount} prerequisites ready • execution blocked",
                color = if (report.isReadyForWindowsExecution) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            report.checks.forEach { check ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(check.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(check.detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (check.ready) "READY" else "BLOCKED",
                        fontSize = 10.sp,
                        color = if (check.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
            Text(
                "This is a prerequisite report, not a runtime test. Windows execution stays disabled until all checks are backed by real installed components.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable private fun RuntimeInventoryCard(context: Context) {
    val report = remember(context) {
        // The installer stores packages under filesDir/runtimes. No trusted
        // release manifest is configured yet, so inspection must remain blocked.
        RuntimeComponentInventory(java.io.File(context.filesDir, "runtimes")).inspect(null)
    }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Runtime file inventory", fontWeight = FontWeight.Bold)
            Text(
                when (report.state) {
                    RuntimeInventoryState.NOT_CONFIGURED -> "Manifest not configured"
                    RuntimeInventoryState.NOT_INSTALLED -> "Runtime not installed"
                    RuntimeInventoryState.INCOMPLETE -> "Runtime incomplete"
                    RuntimeInventoryState.FILES_PRESENT_UNVERIFIED -> "Files present • unverified"
                    RuntimeInventoryState.INVALID_LAYOUT -> "Unsafe or invalid runtime layout"
                },
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            Text(report.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Inventory is read-only. No runtime files are launched, and file presence alone cannot establish package integrity.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable private fun Header(subtitle: String) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text("MTK WINLATOR", fontSize = 25.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun GamesPage(games: List<GameEntry>, pickExe: () -> Unit, remove: (GameEntry) -> Unit, setProfile: (GameEntry, String) -> Unit, setGraphicsBackend: (GameEntry, String) -> Unit, setResolution: (GameEntry, Int, Int) -> Unit, setMemoryLimit: (GameEntry, Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header("MediaTek Mali Edition")
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Runtime", fontWeight = FontWeight.Bold)
            Text("Not installed", color = MaterialTheme.colorScheme.error)
            Text("Wine, Box64 and the Mali graphics runtime are not integrated yet.", fontSize = 13.sp)
            Text("Runtime installation is disabled until distributable packages and licenses are verified.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        RuntimeReadinessCard()
        Button(onClick = pickExe, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Windows EXE") }
        Text("Game library (${games.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (games.isEmpty()) {
            Card { Column(Modifier.padding(16.dp)) { Text("Library is empty"); Text("Add executable files to create saved entries.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
        } else games.forEach { entry ->
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("File size: ${formatFileSize(entry.fileSizeBytes)}", fontSize = 12.sp)
                Text(entry.executableUri, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Profile preference (saved per game)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Lite", "Balanced", "Performance").forEach { p ->
                        val selected = entry.profileId.equals(p, ignoreCase = true)
                        if (selected) Button(onClick = { setProfile(entry, p) }) { Text(p, fontSize = 11.sp) }
                        else OutlinedButton(onClick = { setProfile(entry, p) }) { Text(p, fontSize = 11.sp) }
                    }
                }
                Text("Graphics backend preference", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("AUTO", "OPENGL", "VULKAN").forEach { backend ->
                        val selectedBackend = entry.graphicsBackend.equals(backend, ignoreCase = true)
                        if (selectedBackend) Button(onClick = { setGraphicsBackend(entry, backend) }) { Text(backend, fontSize = 10.sp) }
                        else OutlinedButton(onClick = { setGraphicsBackend(entry, backend) }) { Text(backend, fontSize = 10.sp) }
                    }
                }
                Text("Container resolution", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(960 to 540, 1280 to 720, 1600 to 900).forEach { (width, height) ->
                        val selected = entry.resolutionWidth == width && entry.resolutionHeight == height
                        val label = "${width}x${height}"
                        if (selected) Button(onClick = { setResolution(entry, width, height) }) { Text(label, fontSize = 10.sp) }
                        else OutlinedButton(onClick = { setResolution(entry, width, height) }) { Text(label, fontSize = 10.sp) }
                    }
                }
                Text("Memory limit preference", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50, 75, 90).forEach { percent ->
                        if (entry.memoryLimitPercent == percent) Button(onClick = { setMemoryLimit(entry, percent) }) { Text("$percent%", fontSize = 10.sp) }
                        else OutlinedButton(onClick = { setMemoryLimit(entry, percent) }) { Text("$percent%", fontSize = 10.sp) }
                    }
                }
                Text("Saved preferences only; no graphics backend or container limits are applied until runtime integration.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { remove(entry) }) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Remove") }
            } }
        }
        Text("Project status", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Launcher foundation • v0.1.1")
            Text("Multi-entry library and device diagnostics. Runtime execution remains future integration.", fontSize = 13.sp)
        } }
    }
}

@Composable private fun DevicePage(modifier: Modifier = Modifier) {
    val renderer = remember { readOpenGlRenderer() ?: "Unavailable (GL context could not be created)" }
    val context = androidx.compose.ui.platform.LocalContext.current
    val glEsVersion = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo.reqGlEsVersion.let { version -> if (version == 0) null else "${version shr 16}.${version shr 8 and 0xff}" }
    val graphics = AndroidGraphicsCapabilities.read(context, glEsVersion, null, renderer)
    val memory = DeviceDiagnostics.memory(context)
    val storage = DeviceDiagnostics.internalStorage(context)
    val gpuType = when {
        renderer.contains("mali", ignoreCase = true) -> "ARM Mali detected"
        renderer.contains("adreno", ignoreCase = true) -> "Qualcomm Adreno detected"
        renderer.contains("powervr", ignoreCase = true) -> "PowerVR detected"
        else -> "GPU type not classified"
    }
    val rows = listOf(
        "Manufacturer" to Build.MANUFACTURER,
        "Model" to Build.MODEL,
        "Device" to Build.DEVICE,
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "ABIs" to Build.SUPPORTED_ABIS.joinToString(),
        "CPU cores" to Runtime.getRuntime().availableProcessors().toString(),
        "Heap limit" to "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB",
        "OpenGL ES version" to (graphics.glEsVersion ?: "Unknown"),
        "OpenGL renderer" to renderer,
        "GPU classification" to gpuType,
        "RAM total" to (memory?.let { DeviceDiagnostics.formatBytes(it.totalBytes) } ?: "Unknown"),
        "RAM available" to (memory?.let { DeviceDiagnostics.formatBytes(it.availableBytes) } ?: "Unknown"),
        "Low-memory state" to (memory?.lowMemory?.let { if (it) "System reports low memory" else "Normal" } ?: "Unknown"),
        "Internal storage free" to (storage?.let { DeviceDiagnostics.formatBytes(it.first) } ?: "Unknown"),
        "64-bit ABI support" to if (DeviceDiagnostics.is64BitCapable()) "Supported" else "Not reported",
        "Vulkan hardware level" to (graphics.vulkanHardwareLevel?.toString() ?: "Not advertised"),
        "Vulkan hardware version" to (graphics.vulkanVersion?.toString() ?: "Not advertised"),
        "Mali renderer match" to if (graphics.likelyMali) "Yes" else "No / unknown"
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Header("Device diagnostics")
        rows.forEach { (label, value) -> Card { Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Medium)
        } } }
        Text("Vulkan values are Android-advertised feature metadata. They do not confirm driver stability or Windows-game compatibility.", fontSize = 12.sp)
    }
}

private fun readOpenGlRenderer(): String? {
    var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    var context: EGLContext = EGL14.EGL_NO_CONTEXT
    var surface: EGLSurface = EGL14.EGL_NO_SURFACE
    return try {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return null
        val versions = IntArray(2)
        if (!EGL14.eglInitialize(display, versions, 0, versions, 1)) return null
        val attrs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, configs.size, count, 0) || count[0] == 0) return null
        val config = configs[0] ?: return null
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        surface = EGL14.eglCreatePbufferSurface(display, config, intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
        if (context == EGL14.EGL_NO_CONTEXT || surface == EGL14.EGL_NO_SURFACE) return null
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return null
        GLES20.glGetString(GLES20.GL_RENDERER)
    } catch (_: Throwable) { null } finally {
        runCatching { if (display != EGL14.EGL_NO_DISPLAY) EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT) }
        runCatching { if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface) }
        runCatching { if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context) }
        runCatching { if (display != EGL14.EGL_NO_DISPLAY) EGL14.eglTerminate(display) }
    }
}

@Composable private fun SettingsPage(profile: String, onProfile: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header("Settings")
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Profile preference", fontWeight = FontWeight.Bold)
            Text("Saved locally. Profiles currently store preference only and do not affect runtime performance.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("Lite", "Balanced", "Performance").forEach { p ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p)
                    RadioButton(selected = profile == p, onClick = { onProfile(p) })
                }
            }
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("About", fontWeight = FontWeight.Bold)
            Text("MTK Winlator launcher foundation • 0.1.1")
            Text("Independent project. Not affiliated with or an official Winlator build.")
        } }
    }
}
