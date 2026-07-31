package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color; import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.model.ThermalZone; import com.phrolova.corex.shell.Shell; import com.phrolova.corex.tuner.ThermalTuner
import kotlinx.coroutines.delay; import kotlinx.coroutines.launch

@Composable
fun ThermalScreen(zones: List<ThermalZone>, monitorDelayMs: Int = 2000) {
    val scope = rememberCoroutineScope(); var daemon by remember { mutableStateOf(true) }; var toast by remember { mutableStateOf("") }
    var liveZones by remember { mutableStateOf(zones) }

    LaunchedEffect(zones) {
        val s = Shell.exec("getprop init.svc.vendor.thermald 2>/dev/null").output.trim()
        if (s == "running") daemon = true else if (s == "stopped") daemon = false
        while (true) {
            val updated = liveZones.mapIndexed { i, z ->
                val raw = Shell.exec("cat /sys/class/thermal/thermal_zone$i/temp 2>/dev/null").output.toIntOrNull() ?: z.temp
                val mode = Shell.exec("cat /sys/class/thermal/thermal_zone$i/mode 2>/dev/null").output.trim()
                z.copy(temp = raw, mode = mode)
            }
            liveZones = updated
            delay(monitorDelayMs.toLong())
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Thermal Tuning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (toast.isNotEmpty()) { Text(toast, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)) }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Thermal Daemon", fontSize = 13.sp, fontWeight = FontWeight.Medium); Text(if (daemon) "Running" else "Stopped", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                FilledTonalButton(onClick = { scope.launch { if (daemon) { ThermalTuner.stopThermalDaemon(); daemon = false } else { ThermalTuner.startThermalDaemon(); daemon = true }; toast = if (daemon) "Daemon started" else "Daemon stopped" } }) { Icon(if (daemon) Icons.Default.PlayArrow else Icons.Default.Stop, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(if (daemon) "Stop" else "Start", fontSize = 11.sp) }
            }
        }

        Spacer(Modifier.height(8.dp)); Text("Thermal Zones", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        liveZones.forEach { z ->
            val t = z.temp / 1000
            val c = when { t > 70 -> Color(0xFFFF5252); t > 55 -> Color(0xFFFF9800); t > 40 -> Color(0xFFFFEB3B); else -> Color(0xFF4CAF50) }
            Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Thermostat, null, Modifier.size(16.dp), tint = c)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) { Text(z.type, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text(z.mode.ifEmpty { "enabled" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("${t}°C", color = c, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton({ scope.launch { ThermalTuner.disableAllThermalZones(); toast = "All zones disabled" } }, Modifier.weight(1f)) { Text("Disable All", fontSize = 11.sp) }
            OutlinedButton({ scope.launch { ThermalTuner.enableAllThermalZones(); toast = "All zones enabled" } }, Modifier.weight(1f)) { Text("Enable All", fontSize = 11.sp) }
        }
    }
}
