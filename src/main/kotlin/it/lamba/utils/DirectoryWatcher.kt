package it.lamba.utils

import kotlinx.coroutines.*

import kotlinx.coroutines.Dispatchers.IO
import mu.KotlinLogging
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.*

class DirectoryWatcher(private val config: Configuration) : AbstractCoroutineWorker(IO) {

    private val watchService by lazy { FileSystems.getDefault().newWatchService()!! }
    private val watchKeyToDirectoryMap by lazy { HashMap<WatchKey, Path>() }
    private val eventMap
            by lazy {
                HashMap<WatchEvent.Kind<Path>, Event>().apply {
                    put(ENTRY_CREATE, Event.ENTRY_CREATE)
                    put(ENTRY_MODIFY, Event.ENTRY_MODIFY)
                    put(ENTRY_DELETE, Event.ENTRY_DELETE)
                }
            }
    private val delayMap = HashMap<Pair<String, Event>, Job>()
    private val maintenanceJob: Job = GlobalScope.launch {
        while (isActive) {
            delay(10000)
            val iter = delayMap.iterator()
            while (iter.hasNext()) {
                iter.next().apply { if (value.isCompleted) iter.remove() }
            }
        }
    }

    override fun preStartExecution() {
        if (config.preExistingAsCreated)
            config.pathsToWatch.forEach { rootPath ->
                rootPath.toFile()
                    .walkTopDown()
                    .map { it.toPath() }
                    .filter { filePath -> config.isMatchingAllFilters(filePath) }
                    .forEach { filePath -> GlobalScope.launch {
                        config.notifyListeners(Event.ENTRY_CREATE, filePath)
                    } }
            }

        config.pathsToWatch.forEach {
            logger.debug { "Registering $it" }
            val key = it.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )
            watchKeyToDirectoryMap[key] = it
        }
    }

    override suspend fun execute() {
        val key = watchService.take()
        val dir = watchKeyToDirectoryMap[key] ?: return

        for (event in key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                break
            }

            @Suppress("UNCHECKED_CAST")
            val pathEvent = event as WatchEvent<Path>

            val eventType = eventMap[pathEvent.kind()]

            val path = dir.resolve(pathEvent.context())
            if (config.isMatchingAllFilters(path) && eventType != null) {
                logger.debug { "NEW EVENT $eventType" }
                if (eventType == Event.ENTRY_MODIFY) {
                    val possibleEventKey = Pair(path.toString(), Event.ENTRY_CREATE)
                    if(delayMap.containsKey(possibleEventKey) && delayMap[possibleEventKey]!!.isActive){
                        logger.debug { "ITS CREATION EVENT HAS NOT YET BEEN TRIGGERED" }
                        delayMap[possibleEventKey]?.cancel()
                        delayMap[possibleEventKey] = GlobalScope.launch {
                            delay(config.delayTime)
                            if (isActive)
                                config.notifyListeners(Event.ENTRY_CREATE, path)
                        }
                    } else {
                        val currentEventKey = Pair(path.toString(), Event.ENTRY_MODIFY)
                        if(delayMap.containsKey(currentEventKey) && delayMap[currentEventKey]!!.isActive){
                            logger.debug { "NO ACTIVE CREATION EVENT IN QUEUE FOUND, CREATING MODIFY_EVENT" }
                            delayMap[possibleEventKey]?.cancel()
                            delayMap[possibleEventKey] = GlobalScope.launch {
                                delay(config.delayTime)
                                if (isActive)
                                    config.notifyListeners(Event.ENTRY_MODIFY, path)
                            }
                        }
                    }
                } else
                    GlobalScope.launch { config.notifyListeners(eventType, path) }
            }
            if (!key.reset()) {
                watchKeyToDirectoryMap.remove(key)
                if (watchKeyToDirectoryMap.isEmpty()) {
                    break
                }
            }
        }
    }

    override fun preCancellationExecution() {
        watchService.close()
        maintenanceJob.cancel()
        delayMap.clear()
    }

    interface Listener {
        fun onEvent(event: Event, path: Path)
    }

    enum class Event {
        ENTRY_CREATE,
        ENTRY_MODIFY,
        ENTRY_DELETE
    }

    class Configuration {

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

@Suppress("FunctionName")
fun DirectoryWatcher(builder: DirectoryWatcher.Configuration.() -> Unit)
        = DirectoryWatcher(DirectoryWatcher.Configuration().apply(builder))
