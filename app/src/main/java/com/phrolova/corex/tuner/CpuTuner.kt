package com.phrolova.corex.tuner

import com.phrolova.corex.shell.Shell

object CpuTuner {
    suspend fun setGovernor(id: Int, gov: String) = Shell.exec("echo $gov > /sys/devices/system/cpu/cpufreq/policy$id/scaling_governor")
    suspend fun setMaxFreq(id: Int, f: Int) = Shell.exec("echo $f > /sys/devices/system/cpu/cpufreq/policy$id/scaling_max_freq")
    suspend fun setMinFreq(id: Int, f: Int) = Shell.exec("echo $f > /sys/devices/system/cpu/cpufreq/policy$id/scaling_min_freq")
    suspend fun setCoreOnline(c: Int, on: Boolean) = Shell.exec("echo ${if (on) 1 else 0} > /sys/devices/system/cpu/cpu$c/online")
    suspend fun setBoost(on: Boolean) = Shell.exec("echo ${if (on) 1 else 0} > /sys/devices/system/cpu/cpufreq/boost 2>/dev/null; echo ${if (on) 1 else 0} > /proc/sys/kernel/sched_boost 2>/dev/null")
}
