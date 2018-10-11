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
