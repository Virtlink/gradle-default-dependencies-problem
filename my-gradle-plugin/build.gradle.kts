import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Stick with version 1.3.10 because the kotlin-dsl plugin uses that.
    kotlin("jvm") version "1.3.10" apply false
    `java-gradle-plugin`
    `kotlin-dsl`
}
group = "org.plugin"
version = "develop-SNAPSHOT"
repositories {
    mavenCentral()
}
tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
}
val compile by configurations
dependencies {
    compile(kotlin("stdlib"))
}

gradlePlugin {
  plugins {
    create("my-plugin-base") {
      id = "my.plugin.my-plugin-base"
      implementationClass = "my.plugin.MyPluginBase"
    }
    create("my-plugin-feature") {
      id = "my.plugin.my-plugin-feature"
      implementationClass = "my.plugin.MyPluginFeature"
    }
    create("my-plugin-repository") {
      id = "my.plugin.my-plugin-repository"
      implementationClass = "my.plugin.MyPluginRepository"
    }
  }
}