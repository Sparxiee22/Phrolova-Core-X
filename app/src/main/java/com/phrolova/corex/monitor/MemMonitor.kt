package com.phrolova.corex.monitor

import com.phrolova.corex.shell.Shell

data class MemStats(val totalRam: Long, val availableRam: Long, val usedRam: Long, val zramSize: Long, val zramUsed: Long, val swapTotal: Long, val swapUsed: Long, val zramCompressed: Long = 0)

object MemMonitor {
    suspend fun getStats(): MemStats {
        val memRaw = Shell.exec("cat /proc/meminfo 2>/dev/null | grep -E 'MemTotal|MemAvailable|SwapTotal|SwapFree'").output
        val zSize = Shell.exec("cat /sys/block/zram0/disksize 2>/dev/null").output.toLongOrNull() ?: 0
        val zPhys = Shell.exec("cat /sys/block/zram0/mm_stat 2>/dev/null | awk '{print \$3}'").output.toLongOrNull() ?: 0
        var mt = 0L; var ma = 0L; var st = 0L; var sf = 0L
        for (l in memRaw.lines()) {
            when { l.startsWith("MemTotal:") -> mt = l.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull()?.times(1024) ?: 0
                l.startsWith("MemAvailable:") -> ma = l.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull()?.times(1024) ?: 0
                l.startsWith("SwapTotal:") -> st = l.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull()?.times(1024) ?: 0
                l.startsWith("SwapFree:") -> sf = l.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull()?.times(1024) ?: 0 }
        }
        val swapUsed = st - sf  // swap-space perspective
        return MemStats(mt, ma, mt - ma, zSize, swapUsed, st, swapUsed, zPhys)
    }
}
