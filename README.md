# KDirectoryWatcher [![Build Status](https://travis-ci.org/lamba92/kdirectorywatcher.svg?branch=master)](https://travis-ci.org/lamba92/kdirectorywatcher) [![](https://jitpack.io/v/lamba92/kdirectorywatcher.svg)](https://jitpack.io/#lamba92/kdirectorywatcher)


Utility library to monitor changes inside a directory. 


## Usage

```
val watcher = KDirectoryWatcher {
    addPath(path: Path)
    addPath(path: String)
    addPaths(paths: Iterable<Path>)
    addPaths(paths: Iterable<String>)
    addFilter(filter: (Path) -> Boolean)
    addFilters(filters: Iterable<(Path) -> Boolean>)
    setPreExistingAsCreated(value: Boolean)
    setListener(listener: Listener)
}
watcher.start()
// stuff
watcher.stopt() //may have to wait up to 1 second
```

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