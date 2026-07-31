package com.phrolova.corex.shell

import com.phrolova.corex.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Shell {
    suspend fun exec(command: String) = withContext(Dispatchers.IO) {
        val out = RootShell.run(command)
        val firstErr = out.lines().firstOrNull { it.startsWith("ERROR:") }
        ShellResult(out, firstErr ?: "")
    }

    suspend fun execBatch(commands: List<String>) = exec(commands.joinToString(" && "))
}

data class ShellResult(val output: String, val error: String) {
    val isSuccess get() = !output.startsWith("ERROR:") && error.isEmpty()
    val lines get() = output.lines().filter { it.isNotBlank() }
}
