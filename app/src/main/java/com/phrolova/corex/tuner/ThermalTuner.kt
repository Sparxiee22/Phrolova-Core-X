package com.phrolova.corex.tuner

import com.phrolova.corex.shell.Shell

object ThermalTuner {
    suspend fun stopThermalDaemon() = Shell.exec("stop vendor.thermald 2>/dev/null; stop vendor.thermal-hal-2-0 2>/dev/null")
    suspend fun startThermalDaemon() = Shell.exec("start vendor.thermald 2>/dev/null; start vendor.thermal-hal-2-0 2>/dev/null")
    suspend fun disableAllThermalZones() = Shell.exec("for z in /sys/class/thermal/thermal_zone*/mode; do echo disabled > \$z 2>/dev/null; done")
    suspend fun enableAllThermalZones() = Shell.exec("for z in /sys/class/thermal/thermal_zone*/mode; do echo enabled > \$z 2>/dev/null; done")
}
