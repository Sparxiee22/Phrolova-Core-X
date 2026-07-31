package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.model.ZramInfo; import com.phrolova.corex.shell.Shell; import com.phrolova.corex.tuner.ZramTuner
import kotlinx.coroutines.delay; import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ZramScreen(zramInfo: ZramInfo?, monitorDelayMs: Int = 2000) {
    val scope = rememberCoroutineScope()
    var sizeGb by remember { mutableFloatStateOf(4f) }; var algo by remember { mutableStateOf(zramInfo?.compAlgorithm ?: "lz4") }
    var swp by remember { mutableIntStateOf(100) }; var streams by remember { mutableIntStateOf(8) }
    var toast by remember { mutableStateOf("") }
    var swapUsed by remember { mutableStateOf(0L) }; var swapTotal by remember { mutableStateOf(0L) }
    var compressed by remember { mutableStateOf(0L) }; var curSize by remember { mutableStateOf(zramInfo?.disksize ?: 0) }

    LaunchedEffect(zramInfo) {
        if (zramInfo == null) return@LaunchedEffect
        while (true) {
            val meminfo = Shell.exec("cat /proc/meminfo 2>/dev/null").output
            swapTotal = meminfo.lines().firstOrNull { it.startsWith("SwapTotal:") }?.split("\\s+".toRegex())?.getOrNull(1)?.toLongOrNull() ?: 0
            swapUsed = meminfo.lines().firstOrNull { it.startsWith("SwapFree:") }?.split("\\s+".toRegex())?.getOrNull(1)?.toLongOrNull()?.let { swapTotal - it } ?: 0
            curSize = Shell.exec("cat /sys/block/zram0/disksize 2>/dev/null").output.toLongOrNull() ?: curSize
            val mm = Shell.exec("cat /sys/block/zram0/mm_stat 2>/dev/null").output.trim()
            if (mm.isNotBlank()) {
                val parts = mm.split("\\s+".toRegex())
                if (parts.size >= 4) compressed = parts[3].toLongOrNull() ?: 0
            }
            delay(monitorDelayMs.toLong())
        }
    }

    if (zramInfo == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ZRAM not available") }; return }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("ZRAM Tuning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (toast.isNotEmpty()) { Text(toast, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)) }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(if (zramInfo.initState) "Active" else "Inactive", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Size: ${curSize / 1_000_000} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("${swapUsed / 1024} MB", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Used / ${swapTotal / 1024} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(2.dp))
                if (compressed > 0) {
                    Text("Compressed: ${compressed / 1024} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp)); Text("Compression Algorithm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("lz4", "lzo", "lzo-rle", "zstd", "lz4hc", "deflate").forEach { a ->
                FilterChip(selected = algo == a, onClick = { algo = a }, label = { Text(a, fontSize = 9.sp) })
            }
        }

        Spacer(Modifier.height(8.dp)); Text("Disksize", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        val sizes = (1..8).toList()
        var sizeExp by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = sizeExp, onExpandedChange = { sizeExp = it }) {
            OutlinedTextField(value = "${sizeGb.toInt()} GB", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sizeExp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
            ExposedDropdownMenu(expanded = sizeExp, onDismissRequest = { sizeExp = false }) { sizes.forEach { s -> DropdownMenuItem(text = { Text("$s GB") }, onClick = { sizeGb = s.toFloat(); sizeExp = false }) } }
        }

        Spacer(Modifier.height(8.dp)); Text("Swappiness: $swp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        var swpExp by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = swpExp, onExpandedChange = { swpExp = it }) {
            OutlinedTextField(value = "$swp", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(swpExp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
            ExposedDropdownMenu(expanded = swpExp, onDismissRequest = { swpExp = false }) { (0..200 step 10).forEach { v -> DropdownMenuItem(text = { Text("$v") }, onClick = { swp = v; swpExp = false }) } }
        }

        Spacer(Modifier.height(8.dp)); Text("Max Comp Streams: $streams", fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp))
        var strExp by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = strExp, onExpandedChange = { strExp = it }) {
            OutlinedTextField(value = "$streams", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(strExp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
            ExposedDropdownMenu(expanded = strExp, onDismissRequest = { strExp = false }) { (1..16).forEach { v -> DropdownMenuItem(text = { Text("$v") }, onClick = { streams = v; strExp = false }) } }
        }

        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = { scope.launch { val r = ZramTuner.applyConfig(algo, (sizeGb.toLong() * 1024 * 1024 * 1024), streams); ZramTuner.setSwappiness(swp); toast = if (r.isSuccess) "ZRAM applied" else "Failed: ${r.error}" } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Apply & Reset", fontSize = 12.sp) }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton({ scope.launch { ZramTuner.triggerCompact(); toast = "Compacted" } }, Modifier.weight(1f)) { Text("Compact", fontSize = 10.sp, maxLines = 1) }
            OutlinedButton({ scope.launch { ZramTuner.triggerRecompress(); toast = "Recompressed" } }, Modifier.weight(1f)) { Text("Recompress", fontSize = 10.sp, maxLines = 1) }
            Button(onClick = { scope.launch { ZramTuner.resetZram(); toast = "ZRAM reset" } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f)) { Text("Reset", fontSize = 10.sp, maxLines = 1) }
        }
    }
}
