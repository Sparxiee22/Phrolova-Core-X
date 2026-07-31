package com.phrolova.corex.tuner

import com.phrolova.corex.shell.Shell

object MemTuner {
    suspend fun setDirtyRatio(v: Int) = Shell.exec("echo $v > /proc/sys/vm/dirty_ratio")
    suspend fun setDirtyBackground(v: Int) = Shell.exec("echo $v > /proc/sys/vm/dirty_background_ratio")
    suspend fun setVfsCachePressure(v: Int) = Shell.exec("echo $v > /proc/sys/vm/vfs_cache_pressure")
    suspend fun setMinFreeKbytes(v: Int) = Shell.exec("echo $v > /proc/sys/vm/min_free_kbytes")
    suspend fun setLmkMinfree(values: String) = Shell.exec("echo $values > /sys/module/lowmemorykiller/parameters/minfree 2>/dev/null")
    suspend fun getLmkMinfree() = Shell.exec("cat /sys/module/lowmemorykiller/parameters/minfree 2>/dev/null").output.trim()
    suspend fun lmkAvailable() = Shell.exec("ls /sys/module/lowmemorykiller/parameters/minfree 2>/dev/null").output.isNotEmpty()
}
