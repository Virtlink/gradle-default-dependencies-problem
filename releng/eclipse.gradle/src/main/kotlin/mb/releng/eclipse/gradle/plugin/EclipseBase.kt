package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipseBase : Plugin<Project> {
  companion object {
    const val pluginConfigurationName = "eclipsePlugin"
    const val pluginCompileOnlyConfigurationName = "${pluginConfigurationName}CompileOnly"
    const val featureConfigurationName = "eclipseFeature"
    const val repositoryConfigurationName = "eclipseRepository"
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

val Project.pluginConfiguration get() = this.configurations.getByName(EclipseBase.pluginConfigurationName)
val Project.pluginCompileOnlyConfiguration get() = this.configurations.getByName(EclipseBase.pluginCompileOnlyConfigurationName)
val Project.featureConfiguration get() = this.configurations.getByName(EclipseBase.featureConfigurationName)
val Project.repositoryConfiguration get() = this.configurations.getByName(EclipseBase.repositoryConfigurationName)
