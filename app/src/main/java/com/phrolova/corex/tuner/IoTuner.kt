package com.phrolova.corex.tuner

import com.phrolova.corex.shell.Shell

object IoTuner {
    suspend fun setScheduler(dev: String, sched: String) = Shell.exec("echo $sched > /sys/block/$dev/queue/scheduler 2>/dev/null")
    suspend fun setReadAhead(dev: String, kb: Int) = Shell.exec("echo ${kb.coerceIn(64, 8192)} > /sys/block/$dev/queue/read_ahead_kb 2>/dev/null")
    suspend fun setNrRequests(dev: String, n: Int) = Shell.exec("echo ${n.coerceIn(16, 512)} > /sys/block/$dev/queue/nr_requests 2>/dev/null")
}
