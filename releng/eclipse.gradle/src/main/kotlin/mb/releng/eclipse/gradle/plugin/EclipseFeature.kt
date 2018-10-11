package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.mavenize.toMavenVersion
import mb.releng.eclipse.model.BuildProperties
import mb.releng.eclipse.model.Feature
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import java.nio.file.Files

class EclipseFeature : Plugin<Project> {
  override fun apply(project: Project) {
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    project.pluginManager.apply(BasePlugin::class)
    project.pluginManager.apply(EclipseBasePlugin::class)

    // Process feature.xml file.
    val featureXmlFile = project.file("feature.xml").toPath()
    if(Files.isRegularFile(featureXmlFile)) {
      val feature = Feature.read(featureXmlFile)
      // TODO: id is ignored, since we cannot change name of the project any more at this point.
      // Set project version if it it has not been set yet.
      if(project.version == Project.DEFAULT_VERSION) {
        project.version = feature.version.toMavenVersion()
      }
      // Add plugin dependencies.
      for(dependency in feature.dependencies) {
        val depNotation = "${project.group}:${dependency.id}:${dependency.version.toMavenVersion()}"
        project.dependencies.add(EclipseBasePlugin.pluginConfigurationName, depNotation)
      }
    } else {
      error("Cannot configure Eclipse feature; project $project has no 'feature.xml' file")
    }

    // Process build properties file or use defaults.
    val properties = run {
      val propertiesFile = project.file("build.properties").toPath()
      if(Files.isRegularFile(propertiesFile)) {
        BuildProperties.read(propertiesFile)
      } else {
        BuildProperties.eclipseFeatureDefaults()
      }
    }

    // Build the feature.
    val targetDir = project.buildDir.resolve("feature")
    val featureJarTask = project.tasks.create<Jar>("featureJar") {
      destinationDir = targetDir.resolve("features")
      from(project.projectDir) {
        for(resource in properties.binaryIncludes) {
          include(resource)
        }
      }
    }
    val copyPluginsTask = project.tasks.create<Copy>("copyPlugins") {
      destinationDir = targetDir.resolve("plugins")
      from(project.configurations.getByName(EclipseBasePlugin.pluginConfigurationName))
    }
    val jarTask = project.tasks.create<Jar>("jar") {
      dependsOn(featureJarTask, copyPluginsTask)
      from(targetDir)
    }
    project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(jarTask)
    project.artifacts {
      add(EclipseBasePlugin.featureConfigurationName, jarTask)
    }
  }
}
