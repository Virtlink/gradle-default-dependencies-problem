package my.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPluginBase : Plugin<Project> {
    companion object {
        const val pluginConfigurationName = "myPlugin"
        const val featureConfigurationName = "myFeature"
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

val Project.pluginConfiguration get() = this.configurations.getByName(MyPluginBase.pluginConfigurationName)
val Project.featureConfiguration get() = this.configurations.getByName(MyPluginBase.featureConfigurationName)
