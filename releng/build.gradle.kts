import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  // Stick with version 1.2.61 because the kotlin-dsl plugin uses that.
  kotlin("jvm") version "1.2.61" apply false
}
subprojects {
  group = "org.metaborg"
  version = "develop-SNAPSHOT"
  repositories {
    mavenCentral()
    jcenter()
  }
  apply {
    plugin("kotlin")
  }
  tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
  }
  val compile by configurations
  dependencies {
    compile(kotlin("stdlib"))
  }
}
tasks {
  register("buildAll") {
    dependsOn(subprojects.map { it.tasks["build"] })
  }
  register("cleanAll") {
    dependsOn(subprojects.map { it.tasks["clean"] })
  }
}
