package com.phrolova.corex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility; import androidx.compose.animation.core.spring; import androidx.compose.animation.core.tween; import androidx.compose.animation.fadeIn; import androidx.compose.animation.fadeOut; import androidx.compose.animation.slideInHorizontally; import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image; import androidx.compose.foundation.background; import androidx.compose.foundation.clickable; import androidx.compose.foundation.layout.*; import androidx.compose.foundation.shape.CircleShape; import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip; import androidx.compose.ui.draw.shadow; import androidx.compose.ui.layout.ContentScale; import androidx.compose.ui.res.painterResource; import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.phrolova.corex.monitor.DeviceDetector
import com.phrolova.corex.model.DeviceInfo
import com.phrolova.corex.ui.screens.*
import com.phrolova.corex.ui.theme.PhrolovaTheme
import kotlinx.coroutines.launch

enum class Screen { DASHBOARD, CPU, GPU, ZRAM, THERMAL, MEMORY, IO, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { val tm = remember { mutableStateOf("System") }; PhrolovaTheme(tm.value) { PhrolovaMain(tm) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhrolovaMain(themeMode: MutableState<String> = remember { mutableStateOf("System") }) {
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf(DeviceInfo()) }
    var loading by remember { mutableStateOf(true) }
    var screen by remember { mutableStateOf(Screen.DASHBOARD) }
    var monitorDelay by remember { mutableIntStateOf(2000) }

    LaunchedEffect(Unit) { info = DeviceDetector.detectAll(); loading = false }

    if (loading) {
        AnimatedVisibility(visible = true, enter = fadeIn(tween(600))) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.profile_photo), null, Modifier.size(96.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(12.dp))
                    Text("PHROLOVA CORE X", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("by SparxieDev22", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp); Spacer(Modifier.height(8.dp)); Text("Initializing...", fontSize = 13.sp)
                }
            }
        }
    } else {
        var drawerOpen by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Column { Text("Phrolova Core X", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("V3.3.3 by SparxieDev22", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                        navigationIcon = { IconButton(onClick = { drawerOpen = true }) { Icon(Icons.Default.Menu, null) } },
                        actions = { IconButton(onClick = { screen = Screen.SETTINGS; drawerOpen = false }) { Icon(Icons.Default.Settings, null) } })
                }
            ) { pad ->
                Box(Modifier.fillMaxSize().padding(pad)) {
                    when (screen) {
                        Screen.DASHBOARD -> DashboardScreen(info, monitorDelay)
                        Screen.CPU -> CpuScreen(info, monitorDelay)
                        Screen.GPU -> GpuScreen(info.gpu, info.vulkanVersion, monitorDelay)
                        Screen.ZRAM -> ZramScreen(info.zram, monitorDelay)
                        Screen.THERMAL -> ThermalScreen(info.thermalZones, monitorDelay)
                        Screen.MEMORY -> MemoryScreen(monitorDelay)
                        Screen.IO -> IOScreen(info.blockDevices, monitorDelay)
                        Screen.SETTINGS -> SettingsScreen(themeMode, info, { monitorDelay = it }, monitorDelay)
                    }
                }
            }

            AnimatedVisibility(visible = drawerOpen, enter = slideInHorizontally(animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)) { -it } + fadeIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)), exit = slideOutHorizontally(animationSpec = spring(dampingRatio = 0.8f)) { -it } + fadeOut(animationSpec = spring(dampingRatio = 0.8f))) {
                Row(Modifier.fillMaxSize()) {
                    Surface(Modifier.fillMaxWidth(0.65f).fillMaxHeight().shadow(8.dp, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)).clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)), color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                        Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = { drawerOpen = false }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowBack, null, Modifier.size(20.dp)) }
                            Spacer(Modifier.width(6.dp))
                            Image(painterResource(R.drawable.profile_photo), null, Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) { Text("Phrolova Core X", fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("V3.3.3 by SparxieDev22", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        listOf(
                            Screen.DASHBOARD to "Dashboard" to Icons.Default.Home,
                            Screen.CPU to "CPU" to Icons.Default.Memory,
                            Screen.GPU to "GPU" to Icons.Default.Output,
                            Screen.ZRAM to "ZRAM" to Icons.Default.Storage,
                            Screen.THERMAL to "Thermal" to Icons.Default.Thermostat,
                            Screen.MEMORY to "Memory / VM" to Icons.Default.Settings,
                            Screen.IO to "I/O Scheduler" to Icons.Default.Storage,
                            Screen.SETTINGS to "Settings" to Icons.Default.Settings
                        ).forEach { item ->
                            val s = item.first.first; val l = item.first.second; val icon = item.second
                            Card(onClick = { screen = s; drawerOpen = false }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = if (screen == s) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(20.dp), tint = if (screen == s) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface); Spacer(Modifier.width(12.dp)); Text(l, fontSize = 13.sp, fontWeight = if (screen == s) FontWeight.SemiBold else FontWeight.Normal) }
                            }
                        }
                    }
                    }
                    Box(Modifier.fillMaxSize().clickable { drawerOpen = false })
                }
            }
        }
    }
}
