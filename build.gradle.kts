import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    maven
}

group = "it.lamba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile("org.jetbrains.kotlinx","kotlinx-coroutines-core","1.0.1")
    compile(kotlin("stdlib-jdk8"))
    testCompile(kotlin("test-junit"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
    from(sourceSets.getAt("main").allSource)
}

artifacts.add("archives", sourcesJar)