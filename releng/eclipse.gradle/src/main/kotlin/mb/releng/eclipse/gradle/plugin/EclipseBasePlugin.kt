package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipseBasePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configurations.create("plugin")
    project.configurations.create("feature") {
      setTransitive(false)
    }
  }
}
