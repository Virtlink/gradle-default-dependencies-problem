plugins {
  `java-gradle-plugin`
}
gradlePlugin {
  plugins {
    create("eclipse-plugin") {
      id = "mb.releng.eclipse.gradle.eclipse-plugin"
      implementationClass = "mb.releng.eclipse.gradle.EclipsePluginPlugin"
    }
  }
}