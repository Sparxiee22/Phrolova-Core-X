package com.phrolova.corex.monitor

import com.phrolova.corex.model.GpuType
import com.phrolova.corex.shell.Shell

data class GpuStats(val freq: Int, val load: Int, val temp: Int, val governor: String = "")

object GpuMonitor {
    suspend fun getStats(gpuPath: String, gpuType: GpuType): GpuStats {
        val freq = when (gpuType) {
            GpuType.KGSL -> Shell.exec("cat $gpuPath/gpu_clock_mhz 2>/dev/null").output.toIntOrNull() ?: 0
            GpuType.DEVFREQ -> {
                val hz = Shell.exec("cat $gpuPath/cur_freq 2>/dev/null").output.toIntOrNull() ?: 0
                hz / 1_000_000
            }
            else -> 0
        }
        val load = if (gpuType == GpuType.KGSL) {
            val pct = Shell.exec("cat $gpuPath/gpu_busy_percentage 2>/dev/null").output.trim()
            pct.toIntOrNull() ?: pct.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
        } else 0
        val temp = Shell.exec("cat $gpuPath/temp 2>/dev/null || cat $gpuPath/gpu_temp 2>/dev/null").output.toIntOrNull()?.div(1000) ?: 0
        val gov = Shell.exec("cat $gpuPath/governor 2>/dev/null").output.trim().ifEmpty {
            Shell.exec("cat ${gpuPath.replace("devfreq", "kgsl")}/devfreq_governor 2>/dev/null").output.trim()
        }
        return GpuStats(freq, load, temp, gov)
    }
}
