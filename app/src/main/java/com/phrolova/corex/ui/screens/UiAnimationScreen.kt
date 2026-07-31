package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.tuner.PowerHalTuner; import kotlinx.coroutines.launch

@Composable
fun UiAnimationScreen() {
    val scope = rememberCoroutineScope(); var w by remember { mutableFloatStateOf(1f) }; var t by remember { mutableFloatStateOf(1f) }; var a by remember { mutableFloatStateOf(1f) }; var blur by remember { mutableStateOf(false) }; var toast by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("UI & Animation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
        if (toast.isNotEmpty()) { Text(toast, fontSize = 11.sp); Spacer(Modifier.height(4.dp)) }

        Text("Window: ${"%.1f".format(w)}x", fontWeight = FontWeight.SemiBold); Slider(value = w, onValueChange = { w = it }, valueRange = 0f..1.5f, steps = 5)
        Text("Transition: ${"%.1f".format(t)}x", fontWeight = FontWeight.SemiBold); Slider(value = t, onValueChange = { t = it }, valueRange = 0f..1.5f, steps = 5)
        Text("Animator: ${"%.1f".format(a)}x", fontWeight = FontWeight.SemiBold); Slider(value = a, onValueChange = { a = it }, valueRange = 0f..1.5f, steps = 5)

        Spacer(Modifier.height(4.dp))
        FilledTonalButton(onClick = { scope.launch { PowerHalTuner.setWindowScale(w); PowerHalTuner.setTransitionScale(t); PowerHalTuner.setAnimatorScale(a); toast = "Scales applied" } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Apply Scales") }

        Spacer(Modifier.height(8.dp))
        SwitchRow("Disable Blurs", blur) { blur = it; scope.launch { PowerHalTuner.disableBlur(it); toast = "Blur ${if (it) "OFF" else "ON"} (reboot)" } }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: suspend (Boolean) -> Unit) {
    val scope = rememberCoroutineScope(); var s by remember { mutableStateOf(checked) }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f)); Switch(s, { checked -> s = checked; scope.launch { onToggle(checked) } }) }
}
