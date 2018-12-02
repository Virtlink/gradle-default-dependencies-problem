plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
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
