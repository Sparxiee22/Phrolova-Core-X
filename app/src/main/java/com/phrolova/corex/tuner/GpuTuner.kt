package com.phrolova.corex.tuner

import com.phrolova.corex.model.GpuType
import com.phrolova.corex.shell.Shell

object GpuTuner {
    suspend fun setMaxClock(path: String, type: GpuType, freq: Int) = Shell.exec(
        if (type == GpuType.KGSL) "echo $freq > $path/gpu_max_clock" else "echo $freq > $path/max_freq"
    )
    suspend fun setMinClock(path: String, type: GpuType, freq: Int) = Shell.exec(
        if (type == GpuType.KGSL) "echo $freq > $path/gpu_min_clock" else "echo $freq > $path/min_freq"
    )
    suspend fun setGovernor(path: String, gov: String) = Shell.exec("echo $gov > $path/governor 2>/dev/null")
    suspend fun setThrottling(path: String, on: Boolean) = Shell.exec("echo ${if (on) 1 else 0} > $path/throttling 2>/dev/null")
    suspend fun setHwcg(path: String, on: Boolean) = Shell.exec("echo ${if (on) 1 else 0} > $path/hwcg 2>/dev/null")
    suspend fun setSptpPc(path: String, on: Boolean) = Shell.exec("echo ${if (on) 1 else 0} > $path/sptp_pc 2>/dev/null")
    suspend fun setIfpc(path: String, on: Boolean) = Shell.exec("echo ${if (on) 1 else 0} > $path/ifpc 2>/dev/null")
    suspend fun setRenderer(mode: String) {
        val props = when (mode) {
            "Vulkan" -> "setprop persist.sys.graphics.renderer vulkan; setprop debug.hwui.renderer skiavk; setprop debug.renderengine.backend skiavk; setprop persist.graphics.vulkan.disable 0"
            "Skia GL" -> "setprop persist.sys.graphics.renderer skiagl; setprop debug.hwui.renderer skiagl; setprop debug.renderengine.backend skiaglthreaded; setprop persist.graphics.vulkan.disable 1"
            else -> "setprop debug.hwui.renderer $mode"
        }
        val restart = "input keyevent 26; for p in \$(cmd package list packages | grep -v ia.mo | sed 's/package://g'); do cmd activity force-stop \$p 2>/dev/null; done; input keyevent 26"
        Shell.exec("$props; $restart")
    }
}
