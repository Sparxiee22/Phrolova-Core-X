package com.phrolova.corex.monitor

import com.phrolova.corex.shell.Shell

data class CpuStats(val freqs: List<Int>, val totalLoad: Int, val perCoreLoad: List<Int>, val temp: Int, val governors: List<String> = emptyList())

object CpuMonitor {
    suspend fun getStats(clusterPolicies: List<Int>): CpuStats {
        val script = buildString {
            for (p in clusterPolicies) append("cat /sys/devices/system/cpu/cpufreq/policy$p/scaling_cur_freq 2>/dev/null; ")
            append("cat /proc/stat | head -1; cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null | head -1")
        }
        val lines = Shell.exec(script).lines
        val freqs = clusterPolicies.indices.map { lines.getOrElse(it) { "" }.toIntOrNull() ?: 0 }
        val cpuLine = lines.firstOrNull { it.startsWith("cpu ") }
        val load = cpuLine?.let { parseCpu(it) } ?: 0
        val temp = lines.lastOrNull()?.toIntOrNull()?.div(1000) ?: 0
        val govs = clusterPolicies.map { Shell.exec("cat /sys/devices/system/cpu/cpufreq/policy$it/scaling_governor 2>/dev/null").output.trim() }
        return CpuStats(freqs, load, emptyList(), temp, govs)
    }

    private var prevIdle = 0L; private var prevTotal = 0L
    private fun parseCpu(line: String): Int {
        val parts = line.split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }; if (parts.size < 4) return 0
        val idle = parts[3] + (parts.getOrNull(4) ?: 0); val total = parts.sum()
        val dIdle = idle - prevIdle; val dTotal = total - prevTotal; prevIdle = idle; prevTotal = total
        if (dTotal <= 0) return 0
        return ((1.0 - dIdle.toDouble() / dTotal) * 100).toInt().coerceIn(0, 100)
    }
}
