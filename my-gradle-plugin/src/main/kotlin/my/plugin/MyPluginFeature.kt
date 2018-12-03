package my.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create

class MyPluginFeature : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(MyPluginBase::class)
        project.afterEvaluate { configure(this) }
    }

    private fun configure(project: Project) {
        val pluginConfiguration = project.pluginConfiguration

        project.pluginManager.apply(BasePlugin::class)

        pluginConfiguration.defaultDependencies {
            this.add(project.dependencies.create("com.google.code.findbugs", "jsr305", "3.0.2", "default"))
        }

        project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(pluginConfiguration)
    }
}
