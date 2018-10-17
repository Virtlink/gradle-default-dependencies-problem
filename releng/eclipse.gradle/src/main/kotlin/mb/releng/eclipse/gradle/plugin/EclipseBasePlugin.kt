package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipseBasePlugin : Plugin<Project> {
  companion object {
    const val pluginConfigurationName = "eclipse-plugin"
    const val pluginOptionalConfigurationName = "eclipse-plugin-optional"
    const val featureConfigurationName = "eclipse-feature"
    const val repositoryConfigurationName = "eclipse-repository"
  }

  override fun apply(project: Project) {
    project.configurations.create(pluginConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(pluginOptionalConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(featureConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(repositoryConfigurationName) {
      setTransitive(false)
    }
  }
}

val Project.pluginConfiguration get() = this.configurations.getByName(EclipseBasePlugin.pluginConfigurationName)
val Project.pluginOptionalConfiguration get() = this.configurations.getByName(EclipseBasePlugin.pluginOptionalConfigurationName)
val Project.featureConfiguration get() = this.configurations.getByName(EclipseBasePlugin.featureConfigurationName)
val Project.repositoryConfiguration get() = this.configurations.getByName(EclipseBasePlugin.repositoryConfigurationName)
