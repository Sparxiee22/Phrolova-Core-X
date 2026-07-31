package com.phrolova.corex.ui.screens

import androidx.compose.foundation.layout.*; import androidx.compose.foundation.rememberScrollState; import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.model.BlockDevice; import com.phrolova.corex.shell.Shell; import com.phrolova.corex.tuner.IoTuner
import kotlinx.coroutines.delay; import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IOScreen(devices: List<BlockDevice>, monitorDelayMs: Int = 2000) {
    val scope = rememberCoroutineScope(); var toast by remember { mutableStateOf("") }
    var curSched by remember { mutableStateOf("") }; var raKb by remember { mutableIntStateOf(128) }; var nrReq by remember { mutableIntStateOf(64) }

    LaunchedEffect(devices) {
        val d = devices.firstOrNull() ?: return@LaunchedEffect
        while (true) {
            val raw = Shell.exec("cat /sys/block/${d.name}/queue/scheduler 2>/dev/null").output.trim()
            curSched = raw.substringAfter("[").substringBefore("]").ifEmpty { raw }
            raKb = Shell.exec("cat /sys/block/${d.name}/queue/read_ahead_kb 2>/dev/null").output.toIntOrNull() ?: raKb
            nrReq = Shell.exec("cat /sys/block/${d.name}/queue/nr_requests 2>/dev/null").output.toIntOrNull() ?: nrReq
            delay(monitorDelayMs.toLong())
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("I/O Scheduler", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (toast.isNotEmpty()) { Text(toast, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)) }

        devices.forEach { d ->
            Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(d.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(if (d.rotational) "HDD" else "SSD/UFS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Currently: $curSched", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    val scheds = d.availableSchedulers.ifEmpty { listOf(curSched) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        scheds.forEach { s ->
                            FilterChip(selected = curSched == s, onClick = { scope.launch { val r = IoTuner.setScheduler(d.name, s); curSched = s; toast = if (r.isSuccess) "Scheduler: $s" else "Failed" } }, label = { Text(s, fontSize = 9.sp) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Read-ahead", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    var raExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = raExp, onExpandedChange = { raExp = it }) {
                        OutlinedTextField(value = "$raKb KB", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(raExp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
                        ExposedDropdownMenu(expanded = raExp, onDismissRequest = { raExp = false }) { listOf(64, 128, 256, 512, 1024, 2048, 3072, 4096, 6144, 8192).forEach { v -> DropdownMenuItem(text = { Text("$v KB") }, onClick = { scope.launch { IoTuner.setReadAhead(d.name, v) }; raKb = v; raExp = false }) } }
                    }
                }
            }
        }
    }
}
