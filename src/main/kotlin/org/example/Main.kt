package org.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.algorithm.*

private val CELL = 40.dp
private val SHAPE = RoundedCornerShape(6.dp)
private val C_BASE = Color(0xFFF0F0F0)
private val C_CURR = Color(0xFFB3D1FF)
private val C_MATCH = Color(0xFFA8F3A8)

@Composable
private fun CharBox(ch: Char, bg: Color) =
    Surface(Modifier.size(CELL), SHAPE, bg, tonalElevation = 4.dp) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(ch.toString(), fontSize = 18.sp)
        }
    }

@Composable
private fun Tape(text: String, pos: Int, flash: Boolean = false) =
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        text.forEachIndexed { i, c ->
            val bg by animateColorAsState(
                when {
                    flash -> C_MATCH
                    i == pos && pos >= 0 -> C_CURR
                    else -> C_BASE
                }, label = "cell"
            )
            CharBox(c, bg)
        }
    }

private enum class Mode { SUBSTRING, CYCLIC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var text by remember { mutableStateOf("defabc") }
    var pat by remember { mutableStateOf("abcdef") }
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("KMP run", "π-function")
    var mode by remember { mutableStateOf(Mode.SUBSTRING) }

    val kmp = remember { Kmp() }
    var steps by remember { mutableStateOf(emptyList<KmpStep>()) }
    var idx by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(700f) }

    var piSteps by remember { mutableStateOf(emptyList<PiStep>()) }
    var piIdx by remember { mutableStateOf(0) }
    var piPlaying by remember { mutableStateOf(false) }
    var piSpeed by remember { mutableFloatStateOf(500f) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playing, idx, steps, speed) {
        while (playing && isActive) {
            delay(speed.toLong())
            if (idx < steps.lastIndex) idx++ else playing = false
        }
    }
    LaunchedEffect(piPlaying, piIdx, piSteps, piSpeed) {
        while (piPlaying && isActive) {
            delay(piSpeed.toLong())
            if (piIdx < piSteps.lastIndex) piIdx++ else piPlaying = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("KMP Visualizer") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("A (Text)") })
            OutlinedTextField(pat, { pat = it }, Modifier.fillMaxWidth(), label = { Text("B (Pattern)") })

            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }) {
                        Text(t, Modifier.padding(12.dp))
                    }
                }
            }

            when (tab) {
                0 -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Mode.values().forEach { m ->
                            val selected = mode == m
                            Button(
                                onClick = { mode = m },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(m.name.lowercase().replaceFirstChar { it.titlecase() })
                            }
                        }
                        Button(onClick = {
                            if (mode == Mode.SUBSTRING) {
                                steps = kmp.generateSteps(text, pat)
                                idx = 0
                                playing = false
                                scope.launch {
                                    val matches = steps
                                        .filter { it.j == pat.length }
                                        .map { it.i - pat.length + 1 }
                                        .distinct()
                                    if (matches.isNotEmpty()) {
                                        val oneBased = matches.map { it + 1 }
                                        snackbar.showSnackbar("Found at positions: ${oneBased.joinToString()}")
                                    } else {
                                        snackbar.showSnackbar("No match found")
                                    }
                                }
                            } else {
                                val (shift, trace) = kmp.cyclicShift(text, pat)
                                steps = trace
                                idx = 0
                                playing = false
                                scope.launch {
                                    if (shift >= 0) {
                                        snackbar.showSnackbar("Shift = ${shift + 1}")
                                    } else {
                                        snackbar.showSnackbar("Not a cyclic shift")
                                    }
                                }
                            }
                        }) {
                            Text("Init")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { idx-- }, enabled = idx > 0) {
                            Icon(Icons.Default.SkipPrevious, null)
                        }
                        IconToggleButton(
                            checked = playing,
                            onCheckedChange = { playing = it },
                            enabled = steps.isNotEmpty()
                        ) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        }
                        IconButton(onClick = { idx++ }, enabled = idx < steps.lastIndex) {
                            Icon(Icons.Default.SkipNext, null)
                        }
                        IconButton(onClick = { idx = steps.lastIndex }, enabled = idx < steps.lastIndex) {
                            Icon(Icons.Default.FastForward, null)
                        }
                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            valueRange = 200f..1500f,
                            steps = 8,
                            modifier = Modifier.width(140.dp)
                        )
                    }

                    HorizontalDivider()

                    val step = steps.getOrNull(idx)
                    val showText = if (mode == Mode.SUBSTRING) text else text + text
                    Tape(showText, step?.i ?: -1, step?.j == pat.length)
                    Tape(pat, step?.j ?: -1)

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (step?.pi ?: List(pat.length) { 0 }).forEach {
                            Surface(Modifier.size(CELL), SHAPE, C_BASE, tonalElevation = 2.dp) {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    Text(it.toString())
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            piSteps = kmp.piSteps(pat)
                            piIdx = 0
                            piPlaying = false
                        }) {
                            Text("Build π")
                        }
                        IconButton(onClick = { piIdx-- }, enabled = piIdx > 0) {
                            Icon(Icons.Default.SkipPrevious, null)
                        }
                        IconToggleButton(
                            checked = piPlaying,
                            onCheckedChange = { piPlaying = it },
                            enabled = piSteps.isNotEmpty()
                        ) {
                            Icon(if (piPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        }
                        IconButton(onClick = { piIdx++ }, enabled = piIdx < piSteps.lastIndex) {
                            Icon(Icons.Default.SkipNext, null)
                        }
                        IconButton(onClick = { piIdx = piSteps.lastIndex }, enabled = piIdx < piSteps.lastIndex) {
                            Icon(Icons.Default.FastForward, null)
                        }
                        Slider(
                            value = piSpeed,
                            onValueChange = { piSpeed = it },
                            valueRange = 200f..1500f,
                            steps = 8,
                            modifier = Modifier.width(140.dp)
                        )
                    }

                    HorizontalDivider()

                    val p = piSteps.getOrNull(piIdx)
                    Tape(pat, p?.i ?: -1)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (p?.pi ?: List(pat.length) { 0 }).forEach {
                            Surface(Modifier.size(CELL), SHAPE, C_BASE, tonalElevation = 2.dp) {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    Text(it.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(size = DpSize(900.dp, 700.dp))
    ) {
        App()
    }
}
