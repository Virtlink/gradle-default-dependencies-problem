package mb.releng.eclipse.gradle.plugin.internal

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipseBasePlugin : Plugin<Project> {
  companion object {
    const val pluginConfigurationName = "eclipse-plugin"
    const val pluginCompileOnlyConfigurationName = "eclipse-plugin-compile-only"
    const val featureConfigurationName = "eclipse-feature"
    const val repositoryConfigurationName = "eclipse-repository"
  }

  override fun apply(project: Project) {
    project.configurations.create(pluginConfigurationName) {
      setTransitive(true)
    }
    project.configurations.create(pluginCompileOnlyConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(featureConfigurationName) {
      setTransitive(true)
    }
    project.configurations.create(repositoryConfigurationName) {
      setTransitive(false)
    }
  }
}

val Project.pluginConfiguration get() = this.configurations.getByName(EclipseBasePlugin.pluginConfigurationName)
val Project.pluginCompileOnlyConfiguration get() = this.configurations.getByName(EclipseBasePlugin.pluginCompileOnlyConfigurationName)
val Project.featureConfiguration get() = this.configurations.getByName(EclipseBasePlugin.featureConfigurationName)
val Project.repositoryConfiguration get() = this.configurations.getByName(EclipseBasePlugin.repositoryConfigurationName)
