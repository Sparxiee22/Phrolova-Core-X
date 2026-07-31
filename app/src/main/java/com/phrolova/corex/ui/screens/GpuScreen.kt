package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.model.*; import com.phrolova.corex.shell.Shell; import com.phrolova.corex.tuner.GpuTuner
import kotlinx.coroutines.delay; import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GpuScreen(gpuInfo: GpuInfo?, vulkanVersion: String, monitorDelayMs: Int = 2000) {
    val scope = rememberCoroutineScope()
    var renderer by remember { mutableStateOf("Skia GL") }; var toast by remember { mutableStateOf("") }
    var throttleOn by remember { mutableStateOf(gpuInfo?.throttlingEnabled ?: true) }
    var curGov by remember { mutableStateOf(gpuInfo?.currentGovernor ?: "") }
    var curFreqMhz by remember { mutableStateOf(0) }
    var gpuModel by remember { mutableStateOf("") }; var glesVer by remember { mutableStateOf("") }; var egl by remember { mutableStateOf("") }; var profiler by remember { mutableStateOf("") }
    var curLoad by remember { mutableStateOf(0) }
    val freqs = gpuInfo?.availableFrequencies?.sorted() ?: emptyList()
    val maxFreqRaw = gpuInfo?.maxFreq ?: 0; val minFreqRaw = gpuInfo?.minFreq ?: 0
    val isKgsl = gpuInfo?.type == GpuType.KGSL
    val maxFreqMhz = if (isKgsl) maxFreqRaw else maxFreqRaw / 1_000_000
    val minFreqMhz = if (isKgsl) minFreqRaw else minFreqRaw / 1_000_000
    var minI by remember { mutableStateOf(0) }; var maxI by remember { mutableStateOf((freqs.size - 1).coerceAtLeast(0)) }

    LaunchedEffect(gpuInfo) {
        if (gpuInfo == null) return@LaunchedEffect
        val curRenderer = Shell.exec("getprop debug.hwui.renderer 2>/dev/null").output.trim()
        if (curRenderer == "skiavk") renderer = "Vulkan" else if (curRenderer == "skiagl") renderer = "Skia GL"
        gpuModel = Shell.exec("strings /sys/class/devfreq/${gpuInfo.path.removePrefix("/sys/class/devfreq/")}/device/of_node/compatible 2>/dev/null | head -1").output.trim()
        egl = Shell.exec("getprop ro.hardware.egl 2>/dev/null").output.trim()
        profiler = Shell.exec("getprop graphics.gpu.profiler.support 2>/dev/null").output.trim()
        val glesRaw = Shell.exec("getprop ro.opengles.version 2>/dev/null").output.toIntOrNull() ?: 0
        val glesMajor = glesRaw / 65536; val glesMinor = (glesRaw % 65536) / 256; val glesPatch = glesRaw % 256
        glesVer = if (glesPatch > 0) "$glesMajor.$glesMinor.$glesPatch" else "$glesMajor.$glesMinor"
        curGov = Shell.exec("cat ${gpuInfo.path}/governor 2>/dev/null").output.trim().ifEmpty { gpuInfo.currentGovernor }
        val curRaw = if (gpuInfo.type == GpuType.KGSL) (Shell.exec("cat ${gpuInfo.path}/gpu_clock_mhz 2>/dev/null").output.toIntOrNull() ?: 0) else (Shell.exec("cat ${gpuInfo.path}/cur_freq 2>/dev/null").output.toIntOrNull() ?: 0) / 1_000_000
        curFreqMhz = curRaw
        val maxFile = if (isKgsl) "gpu_max_clock" else "max_freq"; val minFile = if (isKgsl) "gpu_min_clock" else "min_freq"
        val curMax = Shell.exec("cat ${gpuInfo.path}/$maxFile 2>/dev/null").output.toIntOrNull() ?: maxFreqRaw
        val curMin = Shell.exec("cat ${gpuInfo.path}/$minFile 2>/dev/null").output.toIntOrNull() ?: minFreqRaw
        if (freqs.isNotEmpty()) { minI = freqs.indexOf(curMin).coerceAtLeast(0); maxI = freqs.indexOf(curMax).coerceAtLeast(0) }
        while (true) {
            curGov = Shell.exec("cat ${gpuInfo.path}/governor 2>/dev/null").output.trim().ifEmpty { curGov }
            curFreqMhz = if (gpuInfo.type == GpuType.KGSL) (Shell.exec("cat ${gpuInfo.path}/gpu_clock_mhz 2>/dev/null").output.toIntOrNull() ?: curFreqMhz) else (Shell.exec("cat ${gpuInfo.path}/cur_freq 2>/dev/null").output.toIntOrNull() ?: curFreqMhz * 1_000_000) / 1_000_000
            if (gpuInfo.type == GpuType.KGSL) curLoad = Shell.exec("cat ${gpuInfo.path}/gpu_busy_percentage 2>/dev/null").output.trim().let { it.toIntOrNull() ?: it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0 }
            delay(monitorDelayMs.toLong())
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("GPU Tuning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (gpuInfo == null) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text("GPU not detected", Modifier.padding(16.dp)) }; return }
        if (toast.isNotEmpty()) { Spacer(Modifier.height(4.dp)); Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) { Text(toast, Modifier.padding(6.dp), fontSize = 11.sp) }; Spacer(Modifier.height(6.dp)) }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    val renderLabel = if (renderer == "Vulkan") "Vulkan: $vulkanVersion" else "Skia GL"
                    Column(Modifier.weight(1f)) { Text(gpuInfo.type.name, fontSize = 13.sp, fontWeight = FontWeight.Medium); Text(renderLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) { Text("${curFreqMhz} MHz", fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("$curLoad%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gov: $curGov", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Max: $maxFreqMhz MHz | Min: $minFreqMhz MHz", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp)); Text("GPU Info", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth()) { Text("GPU Model", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(gpuModel.ifEmpty { "Mali (Mediatek)" }, fontSize = 11.sp) }
                Row(Modifier.fillMaxWidth()) { Text("OpenGL ES", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(glesVer, fontSize = 11.sp) }
                Row(Modifier.fillMaxWidth()) { Text("Vulkan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(vulkanVersion, fontSize = 11.sp) }
                Row(Modifier.fillMaxWidth()) { Text("EGL", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(egl.ifEmpty { "mali" }, fontSize = 11.sp) }
                Row(Modifier.fillMaxWidth()) { Text("GPU Profiler", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(profiler.ifEmpty { "N/A" }, fontSize = 11.sp) }
            }
        }

        Spacer(Modifier.height(8.dp)); Text("Governor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        val govs = gpuInfo.availableGovernors.ifEmpty { listOf(curGov) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            govs.forEach { g ->
                FilterChip(selected = curGov == g, onClick = { if (curGov != g) scope.launch { val r = GpuTuner.setGovernor(gpuInfo.path, g); curGov = g; toast = if (r.isSuccess) "Gov: $g" else "Governor failed" } }, label = { Text(g, fontSize = 9.sp) })
            }
        }

        if (freqs.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); Text("Clock Control", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
            var mni by remember { mutableStateOf(false) }; var mxi by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { mni = true }, Modifier.fillMaxWidth().height(36.dp)) { Text("Min: ${fmtGpuFreq(freqs[minI], isKgsl)}", fontSize = 11.sp, maxLines = 1) }
                    DropdownMenu(expanded = mni, onDismissRequest = { mni = false }) { freqs.forEachIndexed { idx, f -> DropdownMenuItem(text = { Text(fmtGpuFreq(f, isKgsl)) }, onClick = { minI = idx; maxI = maxI.coerceAtLeast(minI); mni = false }) } }
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { mxi = true }, Modifier.fillMaxWidth().height(36.dp)) { Text("Max: ${fmtGpuFreq(freqs[maxI], isKgsl)}", fontSize = 11.sp, maxLines = 1) }
                    DropdownMenu(expanded = mxi, onDismissRequest = { mxi = false }) { freqs.forEachIndexed { idx, f -> DropdownMenuItem(text = { Text(fmtGpuFreq(f, isKgsl)) }, onClick = { maxI = idx; minI = minI.coerceAtMost(maxI); mxi = false }) } }
                }
            }
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = { scope.launch { val r1 = GpuTuner.setMaxClock(gpuInfo.path, gpuInfo.type, freqs[maxI]); val r2 = GpuTuner.setMinClock(gpuInfo.path, gpuInfo.type, freqs[minI]); val maxFile = if (isKgsl) "gpu_max_clock" else "max_freq"; val minFile = if (isKgsl) "gpu_min_clock" else "min_freq"; val newMax = Shell.exec("cat ${gpuInfo.path}/$maxFile 2>/dev/null").output.toIntOrNull(); val newMin = Shell.exec("cat ${gpuInfo.path}/$minFile 2>/dev/null").output.toIntOrNull(); if (newMax != null) { val idx = freqs.indexOf(newMax); if (idx >= 0) maxI = idx }; if (newMin != null) { val idx = freqs.indexOf(newMin); if (idx >= 0) minI = idx }; toast = if (r1.isSuccess || r2.isSuccess) "Applied" else "Clock failed" } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Check, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Apply Clock", fontSize = 12.sp) }
        }

        if (isKgsl) {
            Spacer(Modifier.height(8.dp)); Text("Power Control", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            SwitchRow("Throttling", throttleOn) { throttleOn = it; scope.launch { val r = GpuTuner.setThrottling(gpuInfo.path, it); toast = if (r.isSuccess) "Throttling ${if (it) "ON" else "OFF"}" else "Failed" } }
            SwitchRow("SPTP PC", true) { scope.launch { GpuTuner.setSptpPc(gpuInfo.path, it); toast = "SPTP PC ${if (it) "ON" else "OFF"}" } }
            SwitchRow("HWCG", true) { scope.launch { GpuTuner.setHwcg(gpuInfo.path, it); toast = "HWCG ${if (it) "ON" else "OFF"}" } }
            SwitchRow("IFPC", true) { scope.launch { GpuTuner.setIfpc(gpuInfo.path, it); toast = "IFPC ${if (it) "ON" else "OFF"}" } }
        }

        Spacer(Modifier.height(8.dp)); Text("Render Engine", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        val modes = listOf("Vulkan", "Skia GL")
        var modeExp by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = modeExp, onExpandedChange = { modeExp = it }) {
            OutlinedTextField(value = renderer, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modeExp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
            ExposedDropdownMenu(expanded = modeExp, onDismissRequest = { modeExp = false }) { modes.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { renderer = m; modeExp = false; scope.launch { GpuTuner.setRenderer(m); toast = "Renderer: $m (restart app)" } }) } }
        }
        Text("Changes need app restart to take effect", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: suspend (Boolean) -> Unit) {
    val scope = rememberCoroutineScope(); var s by remember { mutableStateOf(checked) }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f)); Switch(s, { s = it; scope.launch { onToggle(it) } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(label: String, opts: List<String>, cur: String, onSelect: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) { OutlinedTextField(value = cur, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall); ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) { opts.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); exp = false }) } } } }
}

private fun fmtGpuFreq(f: Int, kgsl: Boolean): String {
    val mhz = if (kgsl) f else f / 1_000_000
    return if (mhz >= 1000) "${"%.1f".format(mhz / 1000f)} GHz" else "$mhz MHz"
}
