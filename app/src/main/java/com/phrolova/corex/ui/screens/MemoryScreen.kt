package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.shell.Shell; import com.phrolova.corex.tuner.MemTuner
import kotlinx.coroutines.delay; import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryScreen(monitorDelayMs: Int = 2000) {
    val scope = rememberCoroutineScope()
    var dr by remember { mutableIntStateOf(20) }; var dbr by remember { mutableIntStateOf(10) }
    var vfs by remember { mutableIntStateOf(100) }; var mf by remember { mutableIntStateOf(11110) }
    var toast by remember { mutableStateOf("") }
    var availRam by remember { mutableStateOf(0L) }; var totalRam by remember { mutableStateOf(0L) }
    var lmkValues by remember { mutableStateOf("") }; var lmkAvail by remember { mutableStateOf(false) }
    val lmkPresets = mapOf("Default" to "2560,5120,11520,25600,35840,38400", "Aggressive" to "1280,2560,5760,12800,17920,19200", "Light" to "5120,10240,23040,51200,71680,76800")

    LaunchedEffect(Unit) {
        dr = Shell.exec("cat /proc/sys/vm/dirty_ratio 2>/dev/null").output.toIntOrNull() ?: 20
        lmkAvail = MemTuner.lmkAvailable(); if (lmkAvail) lmkValues = MemTuner.getLmkMinfree()
        while (true) {
            val meminfo = Shell.exec("cat /proc/meminfo 2>/dev/null").output
            totalRam = meminfo.lines().firstOrNull { it.startsWith("MemTotal:") }?.split("\\s+".toRegex())?.getOrNull(1)?.toLongOrNull() ?: 0
            availRam = meminfo.lines().firstOrNull { it.startsWith("MemAvailable:") }?.split("\\s+".toRegex())?.getOrNull(1)?.toLongOrNull() ?: 0
            delay(monitorDelayMs.toLong())
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Memory & VM", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (toast.isNotEmpty()) { Text(toast, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)) }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("RAM", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text("${availRam / 1024 / 1024} GB / ${totalRam / 1024 / 1024} GB", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { (totalRam - availRam).toFloat() / totalRam.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(6.dp), trackColor = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.height(2.dp))
                Text("${((totalRam - availRam).toFloat() / totalRam.coerceAtLeast(1) * 100).toInt()}% used", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp)); Text("VM Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                DropdownSetting("dirty_ratio (%)", dr, (5..50 step 5).toList()) { dr = it }
                Spacer(Modifier.height(6.dp))
                DropdownSetting("dirty_background (%)", dbr, (1..30 step 3).toList()) { dbr = it }
                Spacer(Modifier.height(6.dp))
                DropdownSetting("vfs_cache_pressure", vfs, (0..200 step 25).toList()) { vfs = it }
                Spacer(Modifier.height(6.dp))
                DropdownSetting("min_free_kbytes", mf, listOf(4096, 8192, 12288, 16384, 24576, 32768, 49152, 65536)) { mf = it }
            }
        }

        if (lmkAvail) {
            Spacer(Modifier.height(8.dp)); Text("LMK (Low Memory Killer)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Text("minfree values (6 comma-separated KB values):", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = lmkValues, onValueChange = { lmkValues = it }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { lmkPresets.forEach { (name, vals) -> FilterChip(selected = lmkValues == vals, onClick = { lmkValues = vals }, label = { Text(name, fontSize = 9.sp) }) } }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = { scope.launch { val r1 = MemTuner.setDirtyRatio(dr); val r2 = MemTuner.setDirtyBackground(dbr); val r3 = MemTuner.setVfsCachePressure(vfs); val r4 = MemTuner.setMinFreeKbytes(mf); val r5 = if (lmkAvail && lmkValues.isNotEmpty()) MemTuner.setLmkMinfree(lmkValues) else Shell.exec("true"); val ok = listOf(r1, r2, r3, r4, r5).any { it.isSuccess }; toast = if (ok) "VM settings applied" else "Failed (SELinux blocked)" } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Apply VM", fontSize = 12.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(label: String, value: Int, opts: List<Int>, onChange: (Int) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text("$label: $value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
            OutlinedTextField(value = "$value", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
            ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) { opts.forEach { o -> DropdownMenuItem(text = { Text("$o") }, onClick = { onChange(o); exp = false }) } }
        }
    }
}
