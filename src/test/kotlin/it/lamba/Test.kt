package it.lamba

import it.lamba.utils.DirectoryWatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths

fun main(args: Array<String>) = runBlocking {
    val testDir = Paths.get(System.getProperty("user.dir"), "test")
    testDir.toFile().apply { if(!exists()) mkdirs() }
    DirectoryWatcher {
        addPath(testDir)
        addListener { event, path -> println("${event.name} | $path") }
        delayTime = 100
    }.start()
    repeat(100){
        val f = File(testDir.toString(), "${System.currentTimeMillis()} - TEST.txt")
        f.writeText("TEST 1")
        delay(2000)
        f.writeText("\nTEST 2")
        delay(1000)
        f.writeText("\nTEST 3")
        delay(1000)
        f.writeText("\nTEST 3")
    }
}