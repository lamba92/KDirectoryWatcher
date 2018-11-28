package it.lamba.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.HashMap
import java.util.concurrent.TimeUnit

class KDirectoryWatcher(build: Configuration.() -> Unit) {

    private val config = Configuration().apply(build)
    private val watchService by lazy { FileSystems.getDefault().newWatchService()!! }
    private val watchKeyToDirectoryMap by lazy { HashMap<WatchKey, Path>() }
    private lateinit var job: Job
    private val eventMap
            by lazy {
                HashMap<WatchEvent.Kind<Path>, Event>().apply {
                    put(ENTRY_CREATE, Event.ENTRY_CREATE)
                    put(ENTRY_MODIFY, Event.ENTRY_MODIFY)
                    put(ENTRY_DELETE, Event.ENTRY_DELETE)
                }
            }

    fun start(): KDirectoryWatcher {
        job = GlobalScope.launch(IO) {
            launch(IO) {
                if (config.preExistingAsCreated)
                    triggerAlreadyExistent(config.pathsToWatch)
            }
            config.pathsToWatch.forEach {
                val key = it.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
                )
                watchKeyToDirectoryMap[key] = it
            }
            while (isActive) {
                val key = watchService.take()
                val dir = watchKeyToDirectoryMap[key] ?: continue

                for (event in key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        break
                    }

                    val pathEvent = event as WatchEvent<Path>
                    val kind = pathEvent.kind()

                    val path = dir.resolve(pathEvent.context())
                    if (config.filters.all { it(path) } && eventMap.containsKey(kind)) {
                        launch { config.listener?.onEvent(eventMap[kind]!!, path) }
                    }
                    if (!key.reset()) {
                        watchKeyToDirectoryMap.remove(key)
                        if (watchKeyToDirectoryMap.isEmpty()) {
                            break
                        }
                    }
                }
            }
        }
        return this
    }

    fun stop() = runBlocking {
        job.cancel()
        watchService.close()
        job.join()
    }

    private fun triggerAlreadyExistent(paths: Iterable<Path>) = paths.forEach {
        it.toFile()
            .walkTopDown()
            .map { it.toPath() }
            .forEach { config.listener?.onEvent(Event.ENTRY_CREATE, it) }
    }

    interface Listener {
        fun onEvent(event: Event, path: Path)
    }

    enum class Event {
        ENTRY_CREATE,
        ENTRY_MODIFY,
        ENTRY_DELETE
    }

    inner class Configuration {

        internal val pathsToWatch = HashSet<Path>()
        internal val filters = HashSet<(Path) -> Boolean>()
        internal var preExistingAsCreated = false
        internal var listener: Listener? = null

        fun addPath(path: Path) = pathsToWatch.add(path)
        fun addPath(path: String) = pathsToWatch.add(path.toPath())
        fun addPaths(paths: Iterable<Path>) = pathsToWatch.addAll(paths)
        fun addPaths(paths: Iterable<String>) = paths.forEach { pathsToWatch.add(it.toPath()) }
        fun addFilter(filter: (path: Path) -> Boolean) = filters.add(filter)
        fun addFilters(filters: Iterable<(Path) -> Boolean>) = this.filters.addAll(filters)
        fun setPreExistingAsCreated(value: Boolean) {
            preExistingAsCreated = value
        }
        fun setListener(listener: Listener) {
            this.listener = listener
        }
        fun setListener(listener: (event: Event, path: Path)->Unit) = setListener(object : Listener{
            override fun onEvent(event: Event, path: Path) = listener(event, path)
        })
    }

}

fun String.toPath() = Paths.get(this)!!
