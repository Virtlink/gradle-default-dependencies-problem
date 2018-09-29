plugins {
  `java-gradle-plugin`
}
gradlePlugin {
  plugins {
    create("eclipse-plugin") {
      id = "org.metaborg.eclipse-plugin"
      implementationClass = "mb.releng.eclipse.gradle.EclipsePluginPlugin"
    }
  }
}