package com.phrolova.corex.tuner

import com.phrolova.corex.shell.Shell

object PowerHalTuner {
    suspend fun setWindowScale(v: Float) = Shell.exec("settings put global window_animation_scale $v")
    suspend fun setTransitionScale(v: Float) = Shell.exec("settings put global transition_animation_scale $v")
    suspend fun setAnimatorScale(v: Float) = Shell.exec("settings put global animator_duration_scale $v")
    suspend fun disableBlur(on: Boolean) = Shell.exec("settings put global disable_window_blurs ${if (on) 1 else 0}")
    suspend fun reduceTransparency(on: Boolean) = Shell.exec("settings put global transition_animation_scale ${if (on) 0.5 else 1.0}")
}
