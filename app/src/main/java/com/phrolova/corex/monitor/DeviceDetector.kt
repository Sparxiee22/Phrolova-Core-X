package com.phrolova.corex.monitor

import com.phrolova.corex.model.*
import com.phrolova.corex.shell.Shell

object DeviceDetector {
    private val gpuNameMap = mapOf(
        "mali-natt" to "Mali-G57(MC1)",
        "mali-g57" to "Mali-G57",
        "mali-g52" to "Mali-G52",
        "mali-g76" to "Mali-G76",
        "mali-g77" to "Mali-G77",
        "mali-g78" to "Mali-G78",
        "mali-g310" to "Mali-G310",
        "mali-g610" to "Mali-G610",
        "mali-g615" to "Mali-G615",
        "mali-g710" to "Mali-G710",
        "mali-g715" to "Mali-G715",
        "mali-t860" to "Mali-T860",
        "mali-t880" to "Mali-T880",
        "adreno-506" to "Adreno 506",
        "adreno-509" to "Adreno 509",
        "adreno-512" to "Adreno 512",
        "adreno-530" to "Adreno 530",
        "adreno-540" to "Adreno 540",
        "adreno-615" to "Adreno 615",
        "adreno-616" to "Adreno 616",
        "adreno-618" to "Adreno 618",
        "adreno-619" to "Adreno 619",
        "adreno-620" to "Adreno 620",
        "adreno-630" to "Adreno 630",
        "adreno-640" to "Adreno 640",
        "adreno-642" to "Adreno 642",
        "adreno-650" to "Adreno 650",
        "adreno-660" to "Adreno 660",
        "adreno-730" to "Adreno 730"
    )
    suspend fun detectAll(): DeviceInfo {
        val model = exec("getprop ro.product.model")
        val androidVer = exec("getprop ro.build.version.release")
        val sdk = exec("getprop ro.build.version.sdk").toIntOrNull() ?: 0
        val platform = exec("getprop ro.board.platform")
        val cpuClusters = detectCpu()
        val gpu = detectGpu()
        val zram = detectZram()
        val thermal = detectThermal()
        val blocks = detectBlock()
        val vulkan = detectVulkan()
        val totalCores = cpuClusters.sumOf { it.cores.size }
        val totalRam = exec("cat /proc/meminfo | grep MemTotal | awk '{print \$2}'").toLongOrNull()?.times(1024) ?: 0
        return DeviceInfo(model, androidVer, sdk, platform, totalCores, totalRam, cpuClusters, gpu, zram, thermal, blocks, vulkan)
    }

    private suspend fun exec(cmd: String) = Shell.exec(cmd).output

    private suspend fun detectCpu(): List<CpuCluster> {
        val raw = Shell.exec("for p in /sys/devices/system/cpu/cpufreq/policy*; do echo POLICY=\$(basename \$p); cat \$p/affected_cpus 2>/dev/null; cat \$p/scaling_available_governors 2>/dev/null | tr ' ' ','; cat \$p/scaling_available_frequencies 2>/dev/null | tr ' ' ','; cat \$p/cpuinfo_max_freq 2>/dev/null; cat \$p/cpuinfo_min_freq 2>/dev/null; cat \$p/scaling_governor 2>/dev/null; cat \$p/scaling_max_freq 2>/dev/null; cat \$p/scaling_min_freq 2>/dev/null; cat \$p/scaling_cur_freq 2>/dev/null; echo END; done").output
        val clusters = mutableListOf<CpuCluster>()
        for (block in raw.split("END\n")) {
            if (!block.contains("POLICY=")) continue
            val lines = block.trim().lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("POLICY=") }
            val policyLine = block.lines().firstOrNull { it.startsWith("POLICY=") } ?: continue
            val policyId = policyLine.removePrefix("POLICY=").filter { it.isDigit() }.toIntOrNull() ?: continue
            if (lines.size < 9) continue
            clusters.add(CpuCluster(policyId, lines[0].split(" ").mapNotNull { it.toIntOrNull() }, lines[1].split(",").filter { it.isNotEmpty() }, lines[2].split(",").mapNotNull { it.toIntOrNull() }, lines[3].toIntOrNull() ?: 0, lines[4].toIntOrNull() ?: 0, lines[5], lines[6].toIntOrNull() ?: 0, lines[7].toIntOrNull() ?: 0, lines[8].toIntOrNull() ?: 0))
        }
        return clusters.sortedBy { it.policyId }
    }

    private suspend fun detectGpu(): GpuInfo? {
        val kgsl = exec("ls /sys/class/kgsl/kgsl-0/gpu_model 2>/dev/null")
        if (kgsl.isNotEmpty()) {
            val freqs = exec("cat /sys/class/kgsl/kgsl-0/gpu_available_frequencies 2>/dev/null").split(" ").mapNotNull { it.toIntOrNull() }
            val gov = exec("cat /sys/class/kgsl/kgsl-0/gpu_governor 2>/dev/null")
            val avGovs = exec("cat /sys/class/kgsl/kgsl-0/gpu_available_governors 2>/dev/null").split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            val raw = exec("cat /sys/class/kgsl/kgsl-0/gpu_model 2>/dev/null").trim()
            val gpuKey = raw.lowercase().replace(" ", "-")
            val model = gpuNameMap[gpuKey] ?: raw
            return GpuInfo("/sys/class/kgsl/kgsl-0", GpuType.KGSL, freqs, avGovs, exec("cat /sys/class/kgsl/kgsl-0/gpu_max_clock 2>/dev/null").toIntOrNull() ?: 0, exec("cat /sys/class/kgsl/kgsl-0/gpu_min_clock 2>/dev/null").toIntOrNull() ?: 0, exec("cat /sys/class/kgsl/kgsl-0/gpu_clock_mhz 2>/dev/null").toIntOrNull() ?: 0, gov, 0, 0, exec("cat /sys/class/kgsl/kgsl-0/throttling 2>/dev/null") != "0", gpuModel = model)
        }
        val devpath = exec("ls /sys/class/devfreq/*gpu*/governor 2>/dev/null").lines().firstOrNull()?.replace("/governor", "") ?: return null
        val freqs = exec("cat $devpath/available_frequencies 2>/dev/null").split(" ").mapNotNull { it.toIntOrNull() }
        val govs = exec("cat $devpath/available_governors 2>/dev/null").split(" ").filter { it.isNotEmpty() }
        val raw = exec("cat $devpath/device/of_node/compatible 2>/dev/null | tr '\\0' '\\n' | grep -v '^$' | tail -1").trim().removePrefix("sprd,")
        val model = gpuNameMap[raw.lowercase()] ?: raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        return GpuInfo(devpath, GpuType.DEVFREQ, freqs, govs, exec("cat $devpath/max_freq 2>/dev/null").toIntOrNull() ?: 0, exec("cat $devpath/min_freq 2>/dev/null").toIntOrNull() ?: 0, exec("cat $devpath/cur_freq 2>/dev/null").toIntOrNull() ?: 0, exec("cat $devpath/governor 2>/dev/null"), gpuModel = model)
    }

    private suspend fun detectZram(): ZramInfo? {
        if (Shell.exec("ls /sys/block/zram0/comp_algorithm 2>/dev/null").output.isEmpty()) return null
        val algo = exec("cat /sys/block/zram0/comp_algorithm 2>/dev/null")
        val avail = algo.split(" ").map { it.removeSurrounding("[", "]").trim() }.filter { it.isNotEmpty() }
        val current = algo.substringAfter("[").substringBefore("]").trim()
        val size = exec("cat /sys/block/zram0/disksize 2>/dev/null").toLongOrNull() ?: 0
        val streams = exec("cat /sys/block/zram0/max_comp_streams 2>/dev/null").toIntOrNull() ?: 1
        val init = exec("cat /sys/block/zram0/initstate 2>/dev/null").toIntOrNull() ?: 0
        return ZramInfo(size, 0, current, avail, streams, init == 1)
    }

    private suspend fun detectThermal(): List<ThermalZone> {
        val raw = Shell.exec("for z in /sys/class/thermal/thermal_zone*; do echo ZONE_START; cat \$z/type 2>/dev/null; cat \$z/temp 2>/dev/null; cat \$z/mode 2>/dev/null; echo ZONE_END; done").output
        val zones = mutableListOf<ThermalZone>()
        for (block in raw.split("ZONE_END\n")) {
            if (!block.contains("ZONE_START")) continue
            val lines = block.trim().lines()
            val idx = lines.indexOfFirst { it == "ZONE_START" }
            if (idx < 0 || idx + 3 > lines.size) continue
            zones.add(ThermalZone("tz${zones.size}", lines.getOrElse(idx + 1) { "" }, lines.getOrElse(idx + 2) { "0" }.toIntOrNull() ?: 0, lines.getOrElse(idx + 3) { "" }, ""))
        }
        return zones
    }

    private suspend fun detectBlock(): List<BlockDevice> {
        val raw = Shell.exec("for b in sda sdb sdc mmcblk0 mmcblk1; do test -d /sys/block/\$b && echo \$b && cat /sys/block/\$b/queue/scheduler 2>/dev/null | tr ' ' ',' && cat /sys/block/\$b/queue/read_ahead_kb 2>/dev/null && cat /sys/block/\$b/queue/rotational 2>/dev/null && echo DEV_END; done").output
        val devs = mutableListOf<BlockDevice>()
        for (block in raw.split("DEV_END\n")) {
            val lines = block.trim().lines(); if (lines.size < 4) continue
            val name = lines[0]
            val scheds = lines[1].split(",").map { it.removeSurrounding("[", "]").trim() }.filter { it.isNotEmpty() }
            val curSched = lines[1].substringAfter("[").substringBefore("]").trim()
            devs.add(BlockDevice(name, curSched, scheds, lines[2].toIntOrNull() ?: 128, lines[3] == "1", 64))
        }
        return devs
    }

    private suspend fun detectVulkan(): String {
        val global = exec("dumpsys gpu 2>/dev/null | grep '^vulkanVersion = ' | head -1 | cut -d= -f2").trim()
        if (global.isNotBlank()) {
            val ver = global.toIntOrNull() ?: global.toIntOrNull(16) ?: return global
            val maj = (ver shr 22) and 0x7F; val min = (ver shr 12) and 0x3FF; val patch = ver and 0xFFF
            return if (patch > 0) "$maj.$min.$patch" else "$maj.$min"
        }
        val api = exec("dumpsys gpu 2>/dev/null | grep vulkanApiVersion | head -1 | cut -d= -f2").trim()
        if (api.isNotBlank()) {
            val ver = api.toIntOrNull(16) ?: return api
            val maj = (ver shr 22) and 0x7F; val min = (ver shr 12) and 0x3FF; val patch = ver and 0xFFF
            return if (patch > 0) "$maj.$min.$patch" else "$maj.$min"
        }
        val v = exec("getprop ro.hwui.use_vulkan 2>/dev/null").trim()
        if (v == "true") return "enabled"
        return "N/A"
    }
}
