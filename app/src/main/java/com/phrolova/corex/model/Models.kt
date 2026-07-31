package com.phrolova.corex.model

data class CpuCluster(
    val policyId: Int, val cores: List<Int>,
    val availableGovernors: List<String>, val availableFrequencies: List<Int>,
    val cpuInfoMaxFreq: Int, val cpuInfoMinFreq: Int,
    var currentGovernor: String = "", var currentMaxFreq: Int = 0,
    var currentMinFreq: Int = 0, var currentFreq: Int = 0
)

data class GpuInfo(
    val path: String, val type: GpuType,
    val availableFrequencies: List<Int> = emptyList(),
    val availableGovernors: List<String> = emptyList(),
    val maxFreq: Int = 0, val minFreq: Int = 0,
    val currentFreq: Int = 0, val currentGovernor: String = "",
    val maxPwrLevel: Int = 0, val minPwrLevel: Int = 0,
    val throttlingEnabled: Boolean = true, val vulkanVersion: String = "",
    val gpuModel: String = ""
)

enum class GpuType { KGSL, DEVFREQ, UNKNOWN }

data class ZramInfo(
    val disksize: Long = 0, val used: Long = 0,
    val compAlgorithm: String = "", val availableAlgorithms: List<String> = emptyList(),
    val maxCompStreams: Int = 1, val initState: Boolean = false
)

data class ThermalZone(val name: String, val type: String, val temp: Int, val mode: String, val policy: String)

data class BlockDevice(
    val name: String, val scheduler: String = "",
    val availableSchedulers: List<String> = emptyList(),
    val readAheadKb: Int = 128, val rotational: Boolean = false, val nrRequests: Int = 64
)

enum class PerfMode(val label: String) {
    GAMING("Gaming"), BALANCED("Balanced"), POWER_SAVE("Power Save"), CUSTOM("Custom")
}

data class DeviceInfo(
    val model: String = "", val androidVersion: String = "", val sdk: Int = 0,
    val platform: String = "", val totalCores: Int = 0, val totalRam: Long = 0,
    val cpuClusters: List<CpuCluster> = emptyList(), val gpu: GpuInfo? = null,
    val zram: ZramInfo? = null, val thermalZones: List<ThermalZone> = emptyList(),
    val blockDevices: List<BlockDevice> = emptyList(), val vulkanVersion: String = ""
)
