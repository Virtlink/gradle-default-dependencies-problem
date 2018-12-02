package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.project

class EclipseRepository : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(EclipseBase::class)
        project.afterEvaluate { configure(this) }
    }

    private fun configure(project: Project) {
        val featureConfiguration = project.featureConfiguration

        project.pluginManager.apply(BasePlugin::class)

        featureConfiguration.defaultDependencies {
            this.add(project.dependencies.project(":spoofax.eclipse.feature", "eclipsePlugin"))
        }

        project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(featureConfiguration)
    }
}
