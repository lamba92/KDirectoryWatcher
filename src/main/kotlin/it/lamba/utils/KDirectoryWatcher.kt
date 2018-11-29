package it.lamba.utils

import kotlinx.coroutines.*

import kotlinx.coroutines.Dispatchers.IO
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.HashMap

class KDirectoryWatcher(build: Configuration.() -> Unit) {

    private val config = Configuration().apply(build)
    private val watchService by lazy { FileSystems.getDefault().newWatchService()!! }
    private val watchKeyToDirectoryMap by lazy { HashMap<WatchKey, Path>() }
    private lateinit var mainJob: Job
    private val eventMap
            by lazy {
                HashMap<WatchEvent.Kind<Path>, Event>().apply {
                    put(ENTRY_CREATE, Event.ENTRY_CREATE)
                    put(ENTRY_MODIFY, Event.ENTRY_MODIFY)
                    put(ENTRY_DELETE, Event.ENTRY_DELETE)
                }
            }
    private val delayMap = HashMap<String, Job>()
    private val maintenanceJob: Job = GlobalScope.launch {
        while (isActive) {
            delay(10000)
            val iter = delayMap.iterator()
            while (iter.hasNext()) {
                iter.next().apply { if (value.isCompleted) iter.remove() }
            }
        }
    }

    fun start() {
        mainJob = GlobalScope.launch(IO) {

            if (config.preExistingAsCreated)
                config.pathsToWatch.forEach { rootPath ->
                    rootPath.toFile()
                        .walkTopDown()
                        .map { it.toPath() }
                        .filter { filePath -> config.isMatchingAllFilters(filePath) }
                        .forEach { filePath -> launch {
                            config.notifyListeners(Event.ENTRY_CREATE, filePath)
                        } }
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

                    @Suppress("UNCHECKED_CAST")
                    val pathEvent = event as WatchEvent<Path>

                    val eventType = eventMap[pathEvent.kind()]

                    val path = dir.resolve(pathEvent.context())
                    if (config.isMatchingAllFilters(path) && eventType != null) {
                        if (eventType == Event.ENTRY_MODIFY) {
                            delayMap[path.toString()]?.cancel()
                            delayMap[path.toString()] = launch {
                                delay(config.delayTime)
                                if (isActive)
                                    config.notifyListeners(eventType, path)
                            }
                        } else
                            launch { config.notifyListeners(eventType, path) }
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
    }

    fun stop() = runBlocking {
        mainJob.cancel()
        watchService.close()
        maintenanceJob.cancel()
        delayMap.clear()
        mainJob.join()
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
        private val filters = HashSet<(Path) -> Boolean>()
        var preExistingAsCreated = false
        private var listeners = ArrayList<Listener>()
        internal var delayTime = 500L

        fun addPath(path: Path) = pathsToWatch.add(path)
        fun addPath(path: String) = pathsToWatch.add(path.toPath())
        fun addPaths(paths: Iterable<Path>) = pathsToWatch.addAll(paths)
        fun addPaths(paths: Iterable<String>) = paths.forEach { pathsToWatch.add(it.toPath()) }
        fun addFilter(filter: (path: Path) -> Boolean) = filters.add(filter)
        fun addFilters(filters: Iterable<(Path) -> Boolean>) = this.filters.addAll(filters)
        fun addListener(listener: Listener) = listeners.add(listener)
        fun addListener(listener: (event: Event, path: Path) -> Unit) = addListener(object : Listener {
            override fun onEvent(event: Event, path: Path) = listener(event, path)
        })

        internal fun notifyListeners(event: Event, path: Path) {
            listeners.forEach { it.onEvent(event, path) }
        }
        internal fun isMatchingAllFilters(path: Path) = filters.all { it(path) }
    }

}

fun String.toPath() = Paths.get(this)!!
