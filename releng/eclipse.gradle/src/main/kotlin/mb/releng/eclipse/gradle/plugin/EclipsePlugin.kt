package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName

class EclipsePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBase::class)
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    val pluginConfiguration = project.pluginConfiguration

      pluginConfiguration.defaultDependencies {
          val element1 = project.dependencies.create("com.google.code.findbugs", "jsr305", "3.0.2", "default")
          this.addAll(listOf(element1))
      }

    project.pluginManager.apply(JavaLibraryPlugin::class)
    val jarTask = project.tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)

    // Prepare manifest and set it in the JAR task.
    val prepareManifestTask = project.tasks.create("prepareManifestTask") {
      dependsOn(pluginConfiguration)
    }
    jarTask.dependsOn(prepareManifestTask)

  }
}
