import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Stick with version 1.3.10 because the kotlin-dsl plugin uses that.
    kotlin("jvm") version "1.3.10" apply false
    `java-gradle-plugin`
    `kotlin-dsl`
}
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
tasks {
    register("buildAll") {
        dependsOn(build)
    }
}


dependencies {
}
gradlePlugin {
  plugins {
    create("eclipse-base") {
      id = "org.metaborg.eclipse-base"
      implementationClass = "mb.releng.eclipse.gradle.plugin.EclipseBase"
    }
    create("eclipse-plugin") {
      id = "org.metaborg.eclipse-plugin"
      implementationClass = "mb.releng.eclipse.gradle.plugin.EclipsePlugin"
    }
    create("eclipse-feature") {
      id = "org.metaborg.eclipse-feature"
      implementationClass = "mb.releng.eclipse.gradle.plugin.EclipseFeature"
    }
    create("eclipse-repository") {
      id = "org.metaborg.eclipse-repository"
      implementationClass = "mb.releng.eclipse.gradle.plugin.EclipseRepository"
    }
  }
}
kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
