package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.project

class EclipseFeature : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBase::class)
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    val pluginConfiguration = project.pluginConfiguration

    project.pluginManager.apply(BasePlugin::class)

      pluginConfiguration.defaultDependencies {
          val element1 = project.dependencies.create("com.google.code.findbugs", "jsr305", "3.0.2", "default")
          this.addAll(listOf(element1))
      }

    val jarTask = project.tasks.create<Jar>("jar") {
      dependsOn(pluginConfiguration)
    }
    project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(jarTask)
    project.artifacts {
      add(EclipseBase.featureConfigurationName, jarTask)
    }
  }
}
