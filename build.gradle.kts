import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
  var kotlin_version: String by extra
  kotlin_version = "1.2.50"

  repositories {
    mavenCentral()
  }
  dependencies {
    classpath(kotlinModule("gradle-plugin", kotlin_version))
    classpath("com.github.jengelman.gradle.plugins:shadow:2.0.1")
  }
}

apply {
  plugin("kotlin")
  plugin("java-library")
}

plugins {
  java
  id("com.github.johnrengelman.shadow") version "2.0.1"
}

val kotlin_version: String by extra

repositories {
  mavenCentral()
}

dependencies {
  compile(kotlinModule("stdlib-jdk8", kotlin_version))
  compile("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
  compile("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
  compile("org.jetbrains.kotlin:kotlin-test-annotations-common:$kotlin_version")
  compile("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
  compile("org.postgresql:postgresql:42.2.2")
  testCompile("org.mockito:mockito-core:2.15.0")
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
  baseName = project.name + "-all"
}

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}
