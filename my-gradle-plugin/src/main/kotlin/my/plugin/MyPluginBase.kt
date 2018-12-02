package my.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipseBase : Plugin<Project> {
    companion object {
        const val pluginConfigurationName = "eclipsePlugin"
        const val featureConfigurationName = "eclipseFeature"
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

val Project.pluginConfiguration get() = this.configurations.getByName(EclipseBase.pluginConfigurationName)
val Project.featureConfiguration get() = this.configurations.getByName(EclipseBase.featureConfigurationName)
