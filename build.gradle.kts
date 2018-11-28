import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = "it.lamba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile("org.jetbrains.kotlinx","kotlinx-coroutines-core","1.0.1")
    compile("io.github.microutils","kotlin-logging","1.6.20")
    compile(kotlin("stdlib-jdk8"))
    testCompile(kotlin("test-junit"))
    testCompile(kotlin("reflect"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}