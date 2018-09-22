import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.2.70" apply false
}
subprojects {
  group = "org.metaborg"
  version = "0.3.0-SNAPSHOT"
  repositories {
    mavenCentral()
    jcenter()
  }
  apply(plugin = "kotlin")
  tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
  }
}
tasks {
  register("buildAll", GradleBuild::class) {
    setTasks(listOf("build"))
  }
  register("cleanAll", GradleBuild::class) {
    setTasks(listOf("clean"))
  }
}
