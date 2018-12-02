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

        project.pluginManager.apply(JavaLibraryPlugin::class)
        val jarTask = project.tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)

        jarTask.dependsOn(pluginConfiguration)

    }
}
