package com.phrolova.corex.tuner

import com.phrolova.corex.shell.Shell

object ZramTuner {
    suspend fun setAlgorithm(a: String) = Shell.exec("echo $a > /sys/block/zram0/comp_algorithm 2>/dev/null")
    suspend fun setDisksize(bytes: Long) = Shell.exec("echo $bytes > /sys/block/zram0/disksize 2>/dev/null")
    suspend fun setSwappiness(v: Int) = Shell.exec("echo $v > /proc/sys/vm/swappiness")
    suspend fun setMaxCompStreams(v: Int) = Shell.exec("echo $v > /sys/block/zram0/max_comp_streams 2>/dev/null")
    suspend fun triggerCompact() = Shell.exec("echo 1 > /sys/block/zram0/compact 2>/dev/null")
    suspend fun triggerRecompress() = Shell.exec("echo 1 > /sys/block/zram0/recompress 2>/dev/null")
    suspend fun resetZram() = Shell.exec("swapoff /dev/block/zram0 2>/dev/null; echo 1 > /sys/block/zram0/reset 2>/dev/null")
    suspend fun applyConfig(algo: String, bytes: Long, streams: Int) = Shell.exec(
        "swapoff /dev/block/zram0 2>/dev/null; " +
        "echo 1 > /sys/block/zram0/reset 2>/dev/null; " +
        "echo $algo > /sys/block/zram0/comp_algorithm 2>/dev/null; " +
        "echo $bytes > /sys/block/zram0/disksize 2>/dev/null; " +
        "echo $streams > /sys/block/zram0/max_comp_streams 2>/dev/null; " +
        "mkswap /dev/block/zram0 2>/dev/null; " +
        "swapon /dev/block/zram0 2>/dev/null"
    )
}
