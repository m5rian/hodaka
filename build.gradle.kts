import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
}

group = "com.github.m5rian.hodaka"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://m5rian.jfrog.io/artifactory/java")
    maven("https://jitpack.io/")
}

dependencies {
    implementation(group = "net.dv8tion", name = "JDA", version = "4.2.1_255") // JDA
    implementation(group = "com.github.m5rian", name = "JdaCommandHandler", version = "dev_0.4.5") // JDA Command handler
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.3") // Logback classic
    implementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = "2.11.2") // Yaml parser
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "13"
}