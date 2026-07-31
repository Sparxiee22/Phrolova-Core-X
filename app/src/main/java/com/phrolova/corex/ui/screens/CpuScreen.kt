package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.text.style.TextAlign; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.model.CpuCluster; import com.phrolova.corex.model.DeviceInfo; import com.phrolova.corex.shell.Shell
import com.phrolova.corex.tuner.CpuTuner; import kotlinx.coroutines.delay; import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CpuScreen(deviceInfo: DeviceInfo, monitorDelayMs: Int = 2000) {
    val scope = rememberCoroutineScope()
    val clusters = remember { deviceInfo.cpuClusters.toMutableList() }
    var state by remember { mutableStateOf(clusters) }
    var sgov by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var svals by remember { mutableStateOf<Map<Int, Pair<Float, Float>>>(emptyMap()) }
    var toast by remember { mutableStateOf("") }
    var coreFreqs by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var coreLoads by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val totalCores = deviceInfo.totalCores
        var prevIdle = MutableList(totalCores) { 0L }; var prevTotal = MutableList(totalCores) { 0L }
        state.forEach { c ->
            val g = Shell.exec("cat /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_governor 2>/dev/null").output.trim().ifEmpty { c.currentGovernor }
            val cmax = Shell.exec("cat /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_max_freq 2>/dev/null").output.toIntOrNull() ?: c.currentMaxFreq
            val cmin = Shell.exec("cat /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_min_freq 2>/dev/null").output.toIntOrNull() ?: c.currentMinFreq
            val mx = c.availableFrequencies.indexOf(cmax).coerceAtLeast(0); val mn = c.availableFrequencies.indexOf(cmin).coerceAtLeast(0)
            sgov = sgov + (c.policyId to g); svals = svals + (c.policyId to Pair(mn.toFloat(), mx.toFloat()))
            state = state.map { if (it.policyId == c.policyId) it.copy(currentGovernor = g, currentMaxFreq = cmax, currentMinFreq = cmin) else it }.toMutableList()
        }
        while (true) {
            val freqs = (0 until totalCores).associateWith { Shell.exec("cat /sys/devices/system/cpu/cpu$it/cpufreq/scaling_cur_freq 2>/dev/null").output.toIntOrNull() ?: 0 }
            coreFreqs = freqs
            val stat = Shell.exec("cat /proc/stat | grep '^cpu[0-9]'").lines
            val loads = mutableMapOf<Int, Int>()
            for (line in stat) {
                val parts = line.split("\\s+".toRegex())
                val cid = parts[0].removePrefix("cpu").toIntOrNull() ?: continue
                if (cid >= totalCores) continue
                val vals = parts.drop(1).mapNotNull { it.toLongOrNull() }; if (vals.size < 4) continue
                val idle = vals[3] + (vals.getOrNull(4) ?: 0); val total = vals.sum()
                val dIdle = idle - prevIdle[cid]; val dTotal = total - prevTotal[cid]; prevIdle[cid] = idle; prevTotal[cid] = total
                loads[cid] = if (dTotal > 0) ((1.0 - dIdle.toDouble() / dTotal) * 100).toInt().coerceIn(0, 100) else 0
            }
            coreLoads = loads
            state.forEach { c ->
                val gov = Shell.exec("cat /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_governor 2>/dev/null").output.trim()
                val curMin = Shell.exec("cat /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_min_freq 2>/dev/null").output.toIntOrNull() ?: 0
                val curMax = Shell.exec("cat /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_max_freq 2>/dev/null").output.toIntOrNull() ?: 0
                if (gov.isNotEmpty()) {
                    sgov = sgov + (c.policyId to gov)
                    state = state.map { if (it.policyId == c.policyId) it.copy(currentGovernor = gov, currentMaxFreq = curMax, currentMinFreq = curMin) else it }.toMutableList()
                }
            }
            delay(monitorDelayMs.toLong())
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("CPU Tuning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (toast.isNotEmpty()) { Text(toast, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }

        var coreIdx = 0
        state.forEach { cluster ->
            val coresInCluster = cluster.cores
            coresInCluster.forEach { cid ->
                val f = coreFreqs[cid] ?: 0; val l = coreLoads[cid] ?: 0
                val label = "CPU$cid"
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
                        LinearProgressIndicator(progress = { l / 100f }, modifier = Modifier.weight(1f).height(6.dp), trackColor = MaterialTheme.colorScheme.surface)
                        Spacer(Modifier.width(4.dp))
                        Text("$l%", fontSize = 11.sp, modifier = Modifier.width(32.dp))
                        Text(fmtCoreFreq(f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                coreIdx++
            }

            Spacer(Modifier.height(4.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Cluster ${cluster.policyId}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(sgov[cluster.policyId] ?: cluster.currentGovernor, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Governor", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cluster.availableGovernors.forEach { g ->
                            val sel = (sgov[cluster.policyId] ?: cluster.currentGovernor) == g
                            FilterChip(selected = sel, onClick = { if (!sel) scope.launch { sgov = sgov + (cluster.policyId to g); val r = CpuTuner.setGovernor(cluster.policyId, g); state = state.map { if (it.policyId == cluster.policyId) it.copy(currentGovernor = g) else it }.toMutableList(); toast = if (r.isSuccess) "Gov: $g" else "Failed: ${r.error}" } }, label = { Text(g, fontSize = 9.sp) })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val freqs = cluster.availableFrequencies; val s = svals[cluster.policyId] ?: Pair(0f, (freqs.size - 1).toFloat())
                    var mi by remember { mutableStateOf(s.first.toInt().coerceIn(0, freqs.size - 1)) }
                    var ma by remember { mutableStateOf(s.second.toInt().coerceIn(0, freqs.size - 1)) }
                    LaunchedEffect(s) { mi = s.first.toInt().coerceIn(0, freqs.size - 1); ma = s.second.toInt().coerceIn(0, freqs.size - 1) }
                    var mni by remember { mutableStateOf(false) }; var mxi by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { mni = true }, Modifier.fillMaxWidth().height(36.dp)) { Text("Min: ${freqs[mi] / 1000} MHz", fontSize = 11.sp, maxLines = 1) }
                            DropdownMenu(expanded = mni, onDismissRequest = { mni = false }) { freqs.forEachIndexed { idx, f -> DropdownMenuItem(text = { Text("${f / 1000} MHz") }, onClick = { mi = idx; ma = ma.coerceAtLeast(mi); svals = svals + (cluster.policyId to Pair(mi.toFloat(), ma.toFloat())); mni = false }) } }
                        }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { mxi = true }, Modifier.fillMaxWidth().height(36.dp)) { Text("Max: ${freqs[ma] / 1000} MHz", fontSize = 11.sp, maxLines = 1) }
                            DropdownMenu(expanded = mxi, onDismissRequest = { mxi = false }) { freqs.forEachIndexed { idx, f -> DropdownMenuItem(text = { Text("${f / 1000} MHz") }, onClick = { ma = idx; mi = mi.coerceAtMost(ma); svals = svals + (cluster.policyId to Pair(mi.toFloat(), ma.toFloat())); mxi = false }) } }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton({ scope.launch { val r1 = CpuTuner.setMaxFreq(cluster.policyId, freqs[ma]); val r2 = CpuTuner.setMinFreq(cluster.policyId, freqs[mi]); toast = if (r1.isSuccess && r2.isSuccess) "Freq applied" else "Freq failed" } }, Modifier.weight(1f)) { Text("Apply", fontSize = 11.sp) }
                        OutlinedButton({ scope.launch { CpuTuner.setMaxFreq(cluster.policyId, cluster.cpuInfoMaxFreq); CpuTuner.setMinFreq(cluster.policyId, cluster.cpuInfoMinFreq); toast = "Reset done" } }, Modifier.weight(1f)) { Text("Reset", fontSize = 11.sp) }
                    }
                }
            }
        }

    }
}

private fun fmtCoreFreq(hz: Int) = if (hz >= 1_000_000) "${"%.1f".format(hz / 1_000_000f)} GHz" else if (hz > 0) "${hz / 1000} MHz" else "N/A"
