package com.example.j2me.android

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import emulator.core.SimpleKMPInterpreter
import emulator.core.JarLoader
import emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

// ... (keep existing imports)

// Navigation State
enum class AppScreen { LIBRARY, EMULATOR }

// Game Data Model
data class GameInfo(val id: String, val name: String, val filePath: String, val mainClass: String?, val touchSupport: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Init File Logging (intercepts stdout and stderr)
        emulator.core.setupFileLogging(this)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.LIBRARY) }
            var currentGame by remember { mutableStateOf<GameInfo?>(null) }
            
            // List of games stored in the app's internal "games" directory
            var games by remember { mutableStateOf(loadGamesFromDisk()) }

            MaterialTheme {
                Surface(color = MaterialTheme.colors.background) {
                    when (currentScreen) {
                        AppScreen.LIBRARY -> {
                            GameLibraryScreen(
                                games = games,
                                onGameSelected = { game ->
                                    currentGame = game
                                    currentScreen = AppScreen.EMULATOR
                                },
                                onGameAdded = { uris ->
                                    val newGame = importGame(uris)
                                    if (newGame != null) {
                                        games = games + newGame
                                    }
                                },
                                onGameDeleted = { gameToDelete ->
                                    deleteGame(gameToDelete)
                                    games = games.filter { it.id != gameToDelete.id }
                                }
                            )
                        }
                        AppScreen.EMULATOR -> {
                            currentGame?.let { game ->
                                EmulatorScreen(game = game) {
                                    // Make sure to stop the game loop
                                    gameStarted = false
                                    currentScreen = AppScreen.LIBRARY
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadGamesFromDisk(): List<GameInfo> {
        val gamesDir = File(filesDir, "games")
        if (!gamesDir.exists()) gamesDir.mkdirs()

        val loader = JarLoader()
        return gamesDir.listFiles()?.filter { it.extension == "jar" }?.map { file ->
            val mainClass = loader.getMainClassFromManifest(file.absolutePath)
            var gameName = file.nameWithoutExtension
            var touchSupport = false
            
            val jadFile = File(gamesDir, file.nameWithoutExtension + ".jad")
            if (jadFile.exists()) {
                val lines = jadFile.readLines()
                for (line in lines) {
                    if (line.startsWith("MIDlet-Name:")) {
                        gameName = line.substringAfter(":").trim()
                    }
                    if (line.startsWith("MIDlet-Touch-Support:", ignoreCase = true)) {
                        touchSupport = line.substringAfter(":").trim().equals("true", ignoreCase = true)
                    }
                }
            }

            GameInfo(
                id = file.nameWithoutExtension,
                name = gameName,
                filePath = file.absolutePath,
                mainClass = mainClass,
                touchSupport = touchSupport
            )
        } ?: emptyList()
    }

    private fun importGame(uris: List<Uri>): GameInfo? {
        try {
            val gamesDir = File(filesDir, "games")
            if (!gamesDir.exists()) gamesDir.mkdirs()

            var jarUri: Uri? = null
            var jadUri: Uri? = null

            for (uri in uris) {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            val name = it.getString(displayNameIndex)
                            if (name.endsWith(".jad", ignoreCase = true)) {
                                jadUri = uri
                            } else if (name.endsWith(".jar", ignoreCase = true)) {
                                jarUri = uri
                            }
                        }
                    }
                }
            }

            if (jarUri == null && uris.size == 1 && jadUri == null) {
                jarUri = uris[0]
            }

            val safeJarUri = jarUri ?: return null

            val fileNameBase = "game_${System.currentTimeMillis()}"
            val destFile = File(gamesDir, "$fileNameBase.jar")

            contentResolver.openInputStream(safeJarUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            var gameName = "New Game"
            var touchSupport = false

            val safeJadUri = jadUri
            if (safeJadUri != null) {
                val destJad = File(gamesDir, "$fileNameBase.jad")
                contentResolver.openInputStream(safeJadUri)?.use { input ->
                    FileOutputStream(destJad).use { output ->
                        input.copyTo(output)
                    }
                }
                
                val lines = destJad.readLines()
                for (line in lines) {
                    if (line.startsWith("MIDlet-Name:")) {
                        gameName = line.substringAfter(":").trim()
                    }
                    if (line.startsWith("MIDlet-Touch-Support:", ignoreCase = true)) {
                        touchSupport = line.substringAfter(":").trim().equals("true", ignoreCase = true)
                    }
                }
            }
            
            val loader = JarLoader()
            val mainClass = loader.getMainClassFromManifest(destFile.absolutePath)
            
            return GameInfo(
                id = destFile.nameWithoutExtension,
                name = gameName,
                filePath = destFile.absolutePath,
                mainClass = mainClass,
                touchSupport = touchSupport
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun deleteGame(game: GameInfo) {
        try {
            val file = File(game.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun GameLibraryScreen(
    games: List<GameInfo>,
    onGameSelected: (GameInfo) -> Unit,
    onGameAdded: (List<Uri>) -> Unit,
    onGameDeleted: (GameInfo) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onGameAdded(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("J2ME Game Library") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Game")
            }
        }
    ) { padding ->
        if (games.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No games added yet. Click + to add a .jar file.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(games) { game ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onGameSelected(game) },
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = game.name, style = MaterialTheme.typography.h6)
                                Text(
                                    text = "Class: ${game.mainClass ?: "Unknown"}",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { onGameDeleted(game) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Game", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmulatorScreen(game: GameInfo, onBack: () -> Unit) {
    var errorLog by remember { mutableStateOf<String?>(null) }
    // Hardcoded resolution for now, will be dynamic in future game settings
    val gameWidth = 240
    val gameHeight = 320

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Top Bar with Back Button ---
        TopAppBar(
            title = { Text(game.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("<", color = Color.White, modifier = Modifier.padding(start = 8.dp))
                }
            }
        )

        if (errorLog != null) {
            Text(
                text = "Emulator Crashed:\n\n$errorLog",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            // Screen area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        SurfaceView(context).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    // Surface is ready, connect to C++ NDK
                                    NativeGraphicsBridge.setSurface(holder.surface)
                                    // Start game loop in background Thread
                                    startGameLoop(game, gameWidth, gameHeight) { error ->
                                        errorLog = error
                                    }
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    // Disconnect C++ NDK from dying surface
                                    NativeGraphicsBridge.setSurface(null)
                                }
                            })
                        }
                    },
                    // Phase 4: Aspect Ratio Preserving (Letterboxing)
                    modifier = Modifier.aspectRatio(gameWidth.toFloat() / gameHeight.toFloat())
                )
            }

            // Keyboard area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f) // Give remaining space to keypad
                    .background(Color.LightGray)
            ) {
                VirtualKeypad { keyCode ->
                    // For now, just log the key press. 
                    // In Phase 5, we will pass this to the ExecutionEngine or KeyHandler
                    println("[VirtualKeypad] Key Pressed: $keyCode")
                }
            }
        }
    }
}

// J2ME Standard Key Codes
const val KEY_NUM0 = 48
const val KEY_NUM1 = 49
const val KEY_NUM2 = 50
const val KEY_NUM3 = 51
const val KEY_NUM4 = 52
const val KEY_NUM5 = 53
const val KEY_NUM6 = 54
const val KEY_NUM7 = 55
const val KEY_NUM8 = 56
const val KEY_NUM9 = 57
const val KEY_STAR = 42 // '*'
const val KEY_POUND = 35 // '#'
const val KEY_UP = -1
const val KEY_DOWN = -2
const val KEY_LEFT = -3
const val KEY_RIGHT = -4
const val KEY_FIRE = -5
const val KEY_SOFT_LEFT = -6
const val KEY_SOFT_RIGHT = -7

@Composable
fun VirtualKeypad(onKeyPress: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // --- Row 1: Soft Keys & D-Pad UP ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KeyButton("L-Soft", modifier = Modifier.weight(1f)) { onKeyPress(KEY_SOFT_LEFT) }
            Spacer(modifier = Modifier.width(16.dp))
            KeyButton("UP", modifier = Modifier.weight(1f)) { onKeyPress(KEY_UP) }
            Spacer(modifier = Modifier.width(16.dp))
            KeyButton("R-Soft", modifier = Modifier.weight(1f)) { onKeyPress(KEY_SOFT_RIGHT) }
        }

        // --- Row 2: D-Pad LEFT / FIRE / RIGHT ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyButton("LEFT", modifier = Modifier.weight(1f)) { onKeyPress(KEY_LEFT) }
            Spacer(modifier = Modifier.width(8.dp))
            KeyButton("FIRE", modifier = Modifier.weight(1f)) { onKeyPress(KEY_FIRE) }
            Spacer(modifier = Modifier.width(8.dp))
            KeyButton("RIGHT", modifier = Modifier.weight(1f)) { onKeyPress(KEY_RIGHT) }
        }

        // --- Row 3: D-Pad DOWN ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            KeyButton("DOWN", modifier = Modifier.weight(1f)) { onKeyPress(KEY_DOWN) }
            Spacer(modifier = Modifier.weight(1f))
        }

        // --- Number Pad ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyButton("1") { onKeyPress(KEY_NUM1) }
            KeyButton("2\nABC") { onKeyPress(KEY_NUM2) }
            KeyButton("3\nDEF") { onKeyPress(KEY_NUM3) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyButton("4\nGHI") { onKeyPress(KEY_NUM4) }
            KeyButton("5\nJKL") { onKeyPress(KEY_NUM5) }
            KeyButton("6\nMNO") { onKeyPress(KEY_NUM6) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyButton("7\nPQRS") { onKeyPress(KEY_NUM7) }
            KeyButton("8\nTUV") { onKeyPress(KEY_NUM8) }
            KeyButton("9\nWXYZ") { onKeyPress(KEY_NUM9) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KeyButton("*") { onKeyPress(KEY_STAR) }
            KeyButton("0") { onKeyPress(KEY_NUM0) }
            KeyButton("#") { onKeyPress(KEY_POUND) }
        }
    }
}

@Composable
fun KeyButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material.Button(
        onClick = onClick,
        modifier = modifier.padding(4.dp).height(50.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
    }
}

private var gameStarted = false

private fun startGameLoop(game: GameInfo, width: Int, height: Int, onError: (String) -> Unit) {
    if (gameStarted) return
    gameStarted = true

    CoroutineScope(Dispatchers.Default).launch {
        try {
            println("[EmulatorThread] Starting emulator loop with resolution ${width}x${height}...")
            NativeGraphicsBridge.initSDL(width, height)
            
            val interpreter = SimpleKMPInterpreter()
            interpreter.currentJarPath = game.filePath // Fix for Phase 5.2 dynamic class loading
            val loader = JarLoader()
            val mainClassToLoad = game.mainClass ?: throw Exception("META-INF/MANIFEST.MF does not contain MIDlet-1")
            
            println("[EmulatorThread] Loading class: $mainClassToLoad from ${game.filePath}...")
            val classFile = loader.loadClassFromJar(game.filePath, mainClassToLoad) // Real ZIP load
            interpreter.loadClass(classFile.className, classFile.bytes)

            println("[EmulatorThread] Executing J2ME startApp()...")
            interpreter.executeMethod(classFile.className, "<init>", emptyArray())
            interpreter.executeMethod(classFile.className, "startApp", emptyArray())
            
            // Temporary Render Loop to prove C++ NDK to Screen blitting works
            var xAnim = 10
            while(gameStarted) { // Exit loop when back button is pressed
                // Clear to blue
                NativeGraphicsBridge.fillRect(0, 0, width, height, 0xFF0000FF.toInt())
                // Draw a moving green square
                NativeGraphicsBridge.fillRect(xAnim, 150, 50, 50, 0xFF00FF00.toInt())
                NativeGraphicsBridge.presentScreen()
                
                xAnim = (xAnim + 5) % width
                delay(30) // ~33 FPS
            }
            
            println("[EmulatorThread] Exited game loop.")

        } catch (e: Throwable) {
            println("[EmulatorThread] CRASH DETECTED")
            e.printStackTrace()
            onError(e.stackTraceToString())
        }
    }
}
