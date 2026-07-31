package com.phrolova.corex.ui.screens

import androidx.compose.foundation.Canvas; import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.graphics.Path; import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.model.*
import com.phrolova.corex.monitor.*
import com.phrolova.corex.shell.Shell
import com.phrolova.corex.tuner.*
import kotlinx.coroutines.delay; import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(deviceInfo: DeviceInfo, monitorDelayMs: Int = 2000) {
    val scope = rememberCoroutineScope()
    var cpuStats by remember { mutableStateOf(CpuStats(emptyList(), 0, emptyList(), 0, emptyList())) }
    var gpuStats by remember { mutableStateOf(GpuStats(0, 0, 0, "")) }
    var memStats by remember { mutableStateOf(MemStats(0, 0, 0, 0, 0, 0, 0, 0)) }
    var currentMode by remember { mutableStateOf(PerfMode.CUSTOM) }
    var isApplying by remember { mutableStateOf(false) }
    var rootOk by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    var batteryLevel by remember { mutableStateOf(0) }; var batteryTemp by remember { mutableStateOf(0f) }; var batteryStatus by remember { mutableStateOf("") }
    var uptimeSecs by remember { mutableStateOf(0f) }; var kernelVer by remember { mutableStateOf("") }; var storageUsed by remember { mutableStateOf(0L) }; var storageTotal by remember { mutableStateOf(0L) }
    var storageHealthPct by remember { mutableStateOf(-1) }
    var cpuLoadHist by remember { mutableStateOf(listOf<Float>()) }; var cpuFreqHist by remember { mutableStateOf(listOf<Float>()) }
    var gpuLoadHist by remember { mutableStateOf(listOf<Float>()) }; var gpuFreqHist by remember { mutableStateOf(listOf<Float>()) }; var ramPctHist by remember { mutableStateOf(listOf<Float>()) }
    var dirtyRatio by remember { mutableIntStateOf(0) }; var minFree by remember { mutableIntStateOf(0) }; var storageType by remember { mutableStateOf("") }; var cacheClearing by remember { mutableStateOf(false) }; var curSched by remember { mutableStateOf("") }; var curRa by remember { mutableIntStateOf(128) }

    LaunchedEffect(Unit) {
        rootOk = Shell.exec("id").output.contains("uid=0")
        kernelVer = Shell.exec("uname -r").output.trim()
        while (true) {
            cpuStats = CpuMonitor.getStats(deviceInfo.cpuClusters.map { it.policyId })
            deviceInfo.gpu?.let { gpuStats = GpuMonitor.getStats(it.path, it.type) }
            memStats = MemMonitor.getStats()
            batteryLevel = Shell.exec("cat /sys/class/power_supply/battery/capacity").output.toIntOrNull() ?: 0
            val raw = Shell.exec("cat /sys/class/power_supply/battery/temp").output.toIntOrNull() ?: 0
            batteryTemp = raw / 10f
            batteryStatus = Shell.exec("cat /sys/class/power_supply/battery/status").output.trim()
            uptimeSecs = Shell.exec("cat /proc/uptime | awk '{print \$1}'").output.toFloatOrNull() ?: 0f
            val dfOut = Shell.exec("df /data 2>/dev/null | tail -1").output.trim()
            val parts = dfOut.split("\\s+".toRegex())
            if (parts.size >= 4) { storageTotal = (parts[1].toLongOrNull() ?: 0) * 1024; storageUsed = (parts[2].toLongOrNull() ?: 0) * 1024 }
            val healthUfs = Shell.exec("cat /sys/devices/platform/soc/soc:ap-apb/20200000.ufs/health_descriptor/life_time_estimation_a 2>/dev/null").output.trim()
            val healthEmmc = Shell.exec("cat /sys/block/mmcblk0/device/pre_eol_info 2>/dev/null").output.trim()
            val (healthRaw, stType) = if (healthUfs.isNotEmpty()) Pair(healthUfs, "UFS") else if (healthEmmc.isNotEmpty()) Pair(healthEmmc, "eMMC") else Pair("", "")
            storageType = stType
            storageHealthPct = if (healthRaw.startsWith("0x")) { val hex = healthRaw.removePrefix("0x").toIntOrNull(16); if (hex != null && hex <= 11) listOf(100,95,90,85,80,75,70,60,50,30,20,10)[hex] else -1 } else -1
            val rpct = if (memStats.totalRam > 0) (memStats.usedRam.toFloat() / memStats.totalRam * 100f) else 0f
            cpuLoadHist = (cpuLoadHist + cpuStats.totalLoad.toFloat()).takeLast(30)
            cpuFreqHist = (cpuFreqHist + (cpuStats.freqs.firstOrNull()?.toFloat() ?: 0f)).takeLast(30)
            gpuLoadHist = (gpuLoadHist + gpuStats.load.toFloat()).takeLast(30)
            gpuFreqHist = (gpuFreqHist + (gpuStats.freq.toFloat() * 1000f)).takeLast(30)
            ramPctHist = (ramPctHist + rpct).takeLast(30)
            dirtyRatio = Shell.exec("cat /proc/sys/vm/dirty_ratio 2>/dev/null").output.toIntOrNull() ?: dirtyRatio
            minFree = Shell.exec("cat /proc/sys/vm/min_free_kbytes 2>/dev/null").output.toIntOrNull() ?: minFree
            val schedRaw = Shell.exec("cat /sys/block/sda/queue/scheduler 2>/dev/null").output.trim()
            curSched = schedRaw.substringAfter("[").substringBefore("]").ifEmpty { schedRaw }
            curRa = Shell.exec("cat /sys/block/sda/queue/read_ahead_kb 2>/dev/null").output.toIntOrNull() ?: curRa
            delay(monitorDelayMs.toLong())
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (!rootOk) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(4.dp)); Text("No root", fontSize = 11.sp) } }; Spacer(Modifier.height(4.dp)) }
        if (msg.isNotEmpty()) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) { Text(msg, Modifier.padding(6.dp), fontSize = 11.sp) }; Spacer(Modifier.height(6.dp)) }

        Card(Modifier.fillMaxWidth().height(140.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp)) {
                    Text("CPU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${cpuStats.totalLoad}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    MiniLineChart(cpuLoadHist, MaterialTheme.colorScheme.primary, Modifier.weight(1f).fillMaxWidth().padding(vertical = 2.dp))
                    Text(fmtFreq(cpuStats.freqs.firstOrNull() ?: 0), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp)) {
                    Text("RAM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    val rp = if (memStats.totalRam > 0) (memStats.usedRam * 100 / memStats.totalRam).toInt() else 0
                    Text("$rp%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    MiniLineChart(ramPctHist, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f).fillMaxWidth().padding(vertical = 2.dp))
                    Text("${fmtSize(memStats.usedRam)}/${fmtSize(memStats.totalRam)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp)) {
                    Text("GPU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("${gpuStats.load}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    MiniLineChart(gpuLoadHist, MaterialTheme.colorScheme.secondary, Modifier.weight(1f).fillMaxWidth().padding(vertical = 2.dp))
                    Text(if (gpuStats.freq > 0) "${gpuStats.freq} MHz" else "N/A", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Quick Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val modes = PerfMode.entries.filter { it != PerfMode.CUSTOM }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, m ->
                SegmentedButton(selected = currentMode == m, onClick = { if (!isApplying) scope.launch { isApplying = true; currentMode = m; msg = applyMode(m, deviceInfo); isApplying = false } }, shape = SegmentedButtonDefaults.itemShape(index = i, count = modes.size), label = { Text(m.label, fontSize = if (m == PerfMode.POWER_SAVE) 9.sp else 10.sp) })
            }
        }
        if (isApplying) { Spacer(Modifier.height(4.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }

        Spacer(Modifier.height(6.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("CPU & GPU", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                deviceInfo.cpuClusters.forEachIndexed { idx, c ->
                    val f = cpuStats.freqs.getOrElse(idx) { c.currentFreq }
                    val g = cpuStats.governors.getOrElse(idx) { c.currentGovernor }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("P${c.policyId}", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                        LinearProgressIndicator(progress = { (cpuStats.totalLoad / 100f).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(4.dp), trackColor = MaterialTheme.colorScheme.surface)
                        Spacer(Modifier.width(4.dp))
                        Text("${fmtFreq(f)} $g", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                val gpuV = if (gpuStats.freq > 0) "${fmtFreq(gpuStats.freq * 1000)} ${gpuStats.governor}" else "N/A"
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("GPU", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    if (gpuStats.load > 0) { LinearProgressIndicator(progress = { (gpuStats.load / 100f).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(4.dp), trackColor = MaterialTheme.colorScheme.surface); Spacer(Modifier.width(4.dp)) }
                    Text(gpuV, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }

                HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(4.dp))
                    Text("MEMORY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(Modifier.height(4.dp))
                val ramPct = ((memStats.usedRam.toDouble() / memStats.totalRam.coerceAtLeast(1)) * 100).toInt()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("RAM", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    LinearProgressIndicator(progress = { (ramPct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(4.dp), trackColor = MaterialTheme.colorScheme.surface)
                    Spacer(Modifier.width(4.dp))
                    Text("${fmtSize(memStats.usedRam)}/${fmtSize(memStats.totalRam)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val zramPct = if (memStats.zramSize > 0) ((memStats.zramUsed.toDouble() / memStats.zramSize.coerceAtLeast(1)) * 100).toInt() else 0
                if (memStats.zramSize > 0) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("ZRAM", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                        LinearProgressIndicator(progress = { (zramPct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(4.dp), trackColor = MaterialTheme.colorScheme.surface)
                        Spacer(Modifier.width(4.dp))
                        Text("${fmtSize(memStats.zramUsed)}/${fmtSize(memStats.zramSize)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(4.dp))
                FilledTonalButton(onClick = { if (!cacheClearing) scope.launch { cacheClearing = true; val r = Shell.exec("sync; cmd package trim-caches 9999999999 2>/dev/null; echo 3 > /proc/sys/vm/drop_caches 2>/dev/null; rm -rf /data/cache/* 2>/dev/null; rm -rf /cache/* 2>/dev/null; sync"); msg = if (r.isSuccess) "All cache cleared" else "Cache clear failed"; cacheClearing = false } }, modifier = Modifier.fillMaxWidth()) { if (cacheClearing) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) } else { Icon(Icons.Default.DeleteSweep, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }; Text(if (cacheClearing) "Clearing..." else "Clear All Cache", fontSize = 11.sp) }

                HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("SYSTEM", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("TMP", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    Text("${cpuStats.temp}°C", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("dirty_ratio: $dirtyRatio%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("min_free: ${minFree / 1024} MB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth()) {
                    Text("I/O", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    Text("sda: $curSched", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("RA: ${curRa / 1024} MB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("DEVICE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Baterai", fontSize = 11.sp, modifier = Modifier.width(54.dp))
                    LinearProgressIndicator(progress = { batteryLevel / 100f }, modifier = Modifier.weight(1f).height(4.dp), trackColor = MaterialTheme.colorScheme.surface)
                    Spacer(Modifier.width(4.dp))
                    Text("$batteryLevel% ${batteryTemp}°C", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Kernel", fontSize = 11.sp, modifier = Modifier.width(54.dp))
                    Text(kernelVer, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(fmtUptime(uptimeSecs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val storagePct = if (storageTotal > 0) ((storageUsed.toDouble() / storageTotal) * 100).toInt() else 0
                if (storageTotal > 0) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Storage", fontSize = 11.sp, modifier = Modifier.width(54.dp))
                        LinearProgressIndicator(progress = { (storagePct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(4.dp), trackColor = MaterialTheme.colorScheme.surface)
                        Spacer(Modifier.width(4.dp))
                        Text("${fmtSize(storageUsed)}/${fmtSize(storageTotal)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (storageHealthPct >= 0) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${storageType.ifEmpty { "Storage" }} Health", fontSize = 11.sp, modifier = Modifier.width(54.dp))
                        val shc = when { storageHealthPct >= 80 -> MaterialTheme.colorScheme.primary; storageHealthPct >= 50 -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.error }
                        LinearProgressIndicator(progress = { storageHealthPct / 100f }, modifier = Modifier.weight(1f).height(4.dp), trackColor = MaterialTheme.colorScheme.surface, color = shc)
                        Spacer(Modifier.width(4.dp))
                        Text("${storageHealthPct}%", fontSize = 10.sp, color = shc)
                    }
                }
                Text("${deviceInfo.model} · Android ${deviceInfo.androidVersion}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }


    }
}

@Composable
fun MiniLineChart(data: List<Float>, color: Color, modifier: Modifier = Modifier, maxValue: Float = 100f) {
    Canvas(modifier) {
        if (data.size < 2) return@Canvas
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val path = Path()
        val pathFill = Path()
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - (v / maxValue.coerceAtLeast(0.01f)) * size.height
            if (i == 0) { path.moveTo(x, y); pathFill.moveTo(x, y) } else { path.lineTo(x, y); pathFill.lineTo(x, y) }
        }
        pathFill.lineTo((data.size - 1) * stepX, size.height)
        pathFill.lineTo(0f, size.height)
        pathFill.close()
        drawPath(pathFill, color.copy(alpha = 0.15f))
        drawPath(path, color, style = Stroke(width = 2f))
    }
}

private fun fmtFreq(hz: Int) = if (hz >= 1_000_000) "${"%.1f".format(hz / 1_000_000f)} GHz" else if (hz > 0) "${hz / 1000} MHz" else "N/A"
private fun fmtSize(b: Long) = if (b >= 1_000_000_000) "${"%.1f".format(b / 1_000_000_000f)} GB" else if (b >= 1_000_000) "${b / 1_000_000} MB" else if (b >= 1000) "${b / 1000} KB" else "$b B"
private fun fmtUptime(s: Float): String { val h = (s / 3600).toInt(); val m = ((s % 3600) / 60).toInt(); return "${h}h ${m}m" }

private suspend fun applyMode(mode: PerfMode, info: DeviceInfo): String {
    val cmds = mutableListOf<String>()
    when (mode) {
        PerfMode.GAMING -> {
            info.cpuClusters.forEach { c -> cmds.addAll(listOf("echo performance > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_governor", "echo ${c.cpuInfoMaxFreq} > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_max_freq", "echo ${c.cpuInfoMaxFreq} > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_min_freq")) }
            info.gpu?.let { g -> val mf = g.availableFrequencies.maxOrNull() ?: g.maxFreq; cmds.addAll(listOf("echo performance > ${g.path}/governor", "echo $mf > ${g.path}/gpu_max_clock", "echo $mf > ${g.path}/gpu_min_clock", "echo 0 > ${g.path}/throttling 2>/dev/null")) }
            cmds.addAll(listOf("setprop debug.hwui.renderer skiavk", "setprop debug.composition.type gpu", "settings put global window_animation_scale 0.5"))
        }
        PerfMode.BALANCED -> {
            info.cpuClusters.forEach { c -> cmds.addAll(listOf("echo schedutil > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_governor", "echo ${c.cpuInfoMaxFreq} > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_max_freq", "echo ${c.cpuInfoMinFreq} > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_min_freq")) }
            info.gpu?.let { g -> val mf = g.availableFrequencies.maxOrNull() ?: g.maxFreq; val mif = g.availableFrequencies.minOrNull() ?: g.minFreq; val bgov = if (g.type == GpuType.KGSL) "msm-adreno-tz" else "simple_ondemand"; cmds.addAll(listOf("echo $bgov > ${g.path}/governor", "echo $mf > ${g.path}/gpu_max_clock", "echo $mif > ${g.path}/gpu_min_clock")) }
            cmds.addAll(listOf("setprop debug.hwui.renderer skiagl", "settings put global window_animation_scale 1.0"))
        }
        PerfMode.POWER_SAVE -> {
            info.cpuClusters.forEach { c -> val mif = c.cpuInfoMinFreq; cmds.addAll(listOf("echo powersave > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_governor", "echo $mif > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_max_freq", "echo $mif > /sys/devices/system/cpu/cpufreq/policy${c.policyId}/scaling_min_freq")) }
            info.gpu?.let { g -> val mif = g.availableFrequencies.minOrNull() ?: g.minFreq; cmds.addAll(listOf("echo powersave > ${g.path}/governor", "echo $mif > ${g.path}/gpu_max_clock", "echo $mif > ${g.path}/gpu_min_clock")) }
            cmds.addAll(listOf("setprop debug.hwui.renderer skiagl", "settings put global window_animation_scale 0.5", "echo 0 > /proc/sys/vm/swappiness"))
        }
        PerfMode.CUSTOM -> return "Custom: no changes"
    }
    val r = Shell.execBatch(cmds)
    return if (r.isSuccess) "${mode.label} applied (${cmds.size} cmds)" else "${mode.label} failed"
}
