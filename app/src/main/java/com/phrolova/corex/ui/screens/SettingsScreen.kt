package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phrolova.corex.model.DeviceInfo
import com.phrolova.corex.shell.Shell
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(themeMode: MutableState<String>, deviceInfo: DeviceInfo, onMonitorDelayChange: (Int) -> Unit = {}, currentDelay: Int = 2000) {
    var kernel by remember { mutableStateOf("") }
    var uptime by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kernel = Shell.exec("uname -r").output.trim()
        val u = Shell.exec("cat /proc/uptime").output.trim().split(" ").firstOrNull()?.toFloatOrNull()?.toInt() ?: 0
        val h = u / 3600; val m = (u % 3600) / 60; val s = u % 60
        uptime = "${h}h ${m}m ${s}s"
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!resetting) showResetDialog = false },
            title = { Text("Reset All Tunings") },
            text = {
                if (resetting) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(); Spacer(Modifier.height(8.dp)); Text("Resetting...")
                    }
                } else {
                    Text("Reset CPU governor/freq, GPU governor/freq, ZRAM, VM settings, and I/O scheduler back to default?")
                }
            },
            confirmButton = {
                if (!resetting) {
                    TextButton(onClick = {
                        scope.launch {
                            resetting = true
                            Shell.exec("for p in /sys/devices/system/cpu/cpufreq/policy*; do g=\$(cat \$p/cpuinfo_max_freq); min=\$(cat \$p/cpuinfo_min_freq); echo performance > \$p/scaling_governor 2>/dev/null; echo \$g > \$p/scaling_max_freq 2>/dev/null; echo \$min > \$p/scaling_min_freq 2>/dev/null; done")
                            val devpath = Shell.exec("ls /sys/class/devfreq/*gpu*/governor 2>/dev/null").output.lines().firstOrNull()?.replace("/governor", "")
                            if (devpath != null) {
                                Shell.exec("cat \$devpath/available_governors 2>/dev/null | cut -d' ' -f1 | xargs -r echo > \$devpath/governor 2>/dev/null; cat \$devpath/max_freq 2>/dev/null > \$devpath/max_freq 2>/dev/null")
                            }
                            Shell.exec("echo 20 > /proc/sys/vm/dirty_ratio 2>/dev/null; echo 10 > /proc/sys/vm/dirty_background_ratio 2>/dev/null; echo 100 > /proc/sys/vm/vfs_cache_pressure 2>/dev/null; echo 0 > /proc/sys/vm/min_free_kbytes 2>/dev/null")
                            Shell.exec("echo cfq > /sys/block/sda/queue/scheduler 2>/dev/null; echo 128 > /sys/block/sda/queue/read_ahead_kb 2>/dev/null")
                            resetting = false; showResetDialog = false
                        }
                    }) { Text("Reset") }
                }
            },
            dismissButton = {
                if (!resetting) TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))

        // Monitoring Interval card
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Monitoring Interval", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                var curDelay by remember(currentDelay) { mutableIntStateOf(currentDelay) }
                val options = listOf(1000 to "1s", 2000 to "2s", 3000 to "3s")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (ms, label) ->
                        FilterChip(
                            selected = curDelay == ms,
                            onClick = { curDelay = ms; onMonitorDelayChange(ms) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Theme card
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Theme", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                ThemeOption("System", Icons.Default.SettingsBrightness, "Follow system theme", themeMode)
                ThemeOption("Light", Icons.Default.BrightnessHigh, "Always light mode", themeMode)
                ThemeOption("Dark", Icons.Default.DarkMode, "Always dark mode", themeMode)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Reset card
        Card(
            onClick = { showResetDialog = true },
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RestartAlt, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Reset All Tunings", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("CPU, GPU, ZRAM, VM, I/O", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // About card
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("About", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                AboutRow("App", "Phrolova Core X V3.3.3")
                AboutRow("Developer", "SparxieDev22")
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                AboutRow("Device", deviceInfo.model)
                AboutRow("Android", "${deviceInfo.androidVersion} (SDK ${deviceInfo.sdk})")
                AboutRow("Platform", deviceInfo.platform)
                AboutRow("CPU", "${deviceInfo.totalCores} cores (${deviceInfo.cpuClusters.size} clusters)")
                deviceInfo.gpu?.let { AboutRow("GPU", if (it.gpuModel.isNotEmpty()) it.gpuModel else "${it.type.name} — ${it.availableFrequencies.size} freqs") }
                AboutRow("RAM", "${deviceInfo.totalRam / 1024 / 1024} MB")
                AboutRow("Vulkan", deviceInfo.vulkanVersion)
                if (kernel.isNotEmpty()) AboutRow("Kernel", kernel)
                if (uptime.isNotEmpty()) AboutRow("Uptime", uptime)
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ThemeOption(value: String, icon: ImageVector, desc: String, themeMode: MutableState<String>) {
    val selected = themeMode.value == value
    Card(
        onClick = { themeMode.value = value },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp), tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(value, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) RadioButton(selected = true, onClick = null)
        }
    }
}
