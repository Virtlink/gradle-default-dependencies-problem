package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipseBasePlugin : Plugin<Project> {
  companion object {
    const val pluginConfigurationName = "plugin"
    const val featureConfigurationName = "feature"
  }

  override fun apply(project: Project) {
    project.configurations.create(pluginConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(featureConfigurationName) {
      setTransitive(false)
    }
  }
}
