package it.lamba

import it.lamba.utils.KDirectoryWatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.nio.file.Path

class Tests : KDirectoryWatcher.Listener {

    private val workingDir by lazy{ System.getProperty("user.dir") }

    fun test(){
        val watcher = KDirectoryWatcher {
            setListener(this@Tests)
            addPath(workingDir.toString() + "/filetests")
            addPaths(emptySet<Path>())
            addFilter { it.endsWith("jpg") }
        }
        watcher.start()
        runBlocking {
            repeat(100){
                val f = File(workingDir.toString() + "/filetests", "$it.lol").apply{ createNewFile() }
                delay(1000)
                f.writeText("LOL")
                delay(1000)
            }
        }
    }

    override fun onEvent(event: KDirectoryWatcher.Event, path: Path) {
        if(event == KDirectoryWatcher.Event.ENTRY_MODIFY)
            println("Event: ${event.name} | path: $path")
    }
}