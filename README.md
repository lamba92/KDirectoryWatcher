# KDirectoryWatcher [![Build Status](https://travis-ci.org/lamba92/KDirectoryWatcher.svg?branch=master)](https://travis-ci.org/lamba92/KDirectoryWatcher) [![](https://jitpack.io/v/lamba92/kdirectorywatcher.svg)](https://jitpack.io/#lamba92/kdirectorywatcher)


Utility library to monitor changes inside a directory. It is implemented using coroutines and configurations lambda. 

This project is an adaptation of [Hindol](https://github.com/Hindol) / [commons](https://github.com/Hindol/commons) / [DirectoryWatcher](https://github.com/Hindol/commons/blob/master/src/main/java/com/github/hindol/commons/file/DirectoryWatcher.java)

Written in Kotlin with ❤️.

The library is still work in progress. By now it correctly triggers `ENRTY_CREATE` and `ENTRY_DELETE`. ~~The `ENTRY_MODIFY` is triggered many times, as many times as the OS writes on the file (which is technically correct but tedious for developers).~~

TODO:
- [ ] Tests!
- [x] Add a configurable amount of time between each call with `ENTRY_MODIFY` on the same file
- [x] Multiple listeners
- [ ] Inverse filtering for paths (discarding instead of selecting)
- [ ] A constructor for the Java peasants

## Usage

Create a `KDirectoryWatcher` object and register a listener to it using `setListener(listener: Listener)` method inside the `KDirectoryWatcher.Configuration` extension lambda. 


```
val watcher = KDirectoryWatcher {
    addPath(System.getProperty("user.dir"))
    setListener { event, path -> 
        switch(event){
            ENRTY_CREATE -> // whatever
            ENTRY_MODIFY -> // whatever
            ENTRY_DELETE -> // whatever
        }     
    }
    addFilter { it.endsWith("jpg") }
}

watcher.start()
// stuff
watcher.stop() //may have to wait up to 1 second
```

Each event is ran in a separated coroutine in default dispatcher (which means don't make thread blocking calls inside the listener, if you have to, delegate it to a coroutine in the IO dispatcher).

Here's all the available configuration methods for now:

```
KDirectoryWatcher {
    addPath(path: Path)
    addPath(path: String)
    addPaths(paths: Iterable<Path>)
    addPaths(paths: Iterable<String>)
    addFilter(filter: (Path) -> Boolean)
    addFilters(filters: Iterable<(Path) -> Boolean>)
    setPreExistingAsCreated(value: Boolean)
    setListener(listener: Listener)
}
```

A `filter` is a lambda which accepts the path which triggered the change and if it returns true the event wil be processed. You can have many filters, if so the path must satisfy them all.

## Install [![](https://jitpack.io/v/lamba92/kdirectorywatcher.svg)](https://jitpack.io/#lamba92/kdirectorywatcher)

Add the [JitPack.io](http://jitpack.io) repository to the project `build.grade`:
```
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then import the latest version in the `build.gradle` of the modules you need:

```
dependencies {
    implementation 'com.github.lamba92:kdirectorywatcher:{latest_version}'
}
```

If using Gradle Kotlin DSL:
```
repositories {
    maven(url = "https://jitpack.io")
}
...
dependencies {
    implementation("com.github.lamba92", "kdirectorywatcher", "{latest_version}")
}
```
For Maven:
```
<repositories>
   <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
   </repository>
</repositories>
...
<dependency> 	 
   <groupId>com.github.Lamba92</groupId>
   <artifactId>kdirectorywatcher</artifactId>
   <version>{latest_version}</version>
</dependency>
```
