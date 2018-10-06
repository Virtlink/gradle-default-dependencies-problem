plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
}
dependencies {
  compile(project(":releng.eclipse"))
}
gradlePlugin {
  plugins {
    create("eclipse-plugin") {
      id = "org.metaborg.eclipse-plugin"
      implementationClass = "mb.releng.eclipse.gradle.plugin.EclipsePlugin"
    }
  }
}
