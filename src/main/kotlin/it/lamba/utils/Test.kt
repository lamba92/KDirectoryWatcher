package it.lamba.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path

fun main(args: Array<String>) {

    val workingDir by lazy { System.getProperty("user.dir") }

    val watcher = KDirectoryWatcher {
        addPath(workingDir.toString() + "/filetests")
        addPaths(emptySet<Path>())
        addFilter { it.toFile().isFile }
        addFilter { it.toFile().extension == "lol" }
        delayTime = 200
        addListener { event, path ->
            println("Event: ${event.name} | path: $path")
        }
        preExistingAsCreated = true
    }
    watcher.start()
    runBlocking {
        repeat(100) {
            File(workingDir.toString() + "/filetests").mkdirs()
            val f = File(workingDir.toString() + "/filetests", "$it.lol")
                .apply { createNewFile() }
            delay(1000)
            f.writeText("LOL")
            delay(1000)
        }
    }
}
