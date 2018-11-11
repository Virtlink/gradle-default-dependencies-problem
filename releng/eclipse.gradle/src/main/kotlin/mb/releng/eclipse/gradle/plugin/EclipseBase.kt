package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipseBase : Plugin<Project> {
  companion object {
    const val pluginConfigurationName = "eclipsePlugin"
    const val pluginTransitiveConfigurationName = "${pluginConfigurationName}Transitive"
    const val featureConfigurationName = "eclipseFeature"
    const val repositoryConfigurationName = "eclipseRepository"
  }

  override fun apply(project: Project) {
    project.configurations.create(pluginConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(pluginTransitiveConfigurationName) {
      setTransitive(true)
    }
    project.configurations.create(featureConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(repositoryConfigurationName) {
      setTransitive(false)
    }
  }
}

val Project.pluginTransitiveConfiguration get() = this.configurations.getByName(EclipseBase.pluginTransitiveConfigurationName)
val Project.pluginConfiguration get() = this.configurations.getByName(EclipseBase.pluginConfigurationName)
val Project.featureConfiguration get() = this.configurations.getByName(EclipseBase.featureConfigurationName)
val Project.repositoryConfiguration get() = this.configurations.getByName(EclipseBase.repositoryConfigurationName)
