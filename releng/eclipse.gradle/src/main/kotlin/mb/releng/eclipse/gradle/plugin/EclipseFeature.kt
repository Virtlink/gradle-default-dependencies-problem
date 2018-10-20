package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.plugin.internal.*
import mb.releng.eclipse.gradle.task.EclipseRun
import mb.releng.eclipse.gradle.task.PrepareEclipseRunConfig
import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.gradle.util.toGradleDependency
import mb.releng.eclipse.mavenize.toMaven
import mb.releng.eclipse.model.eclipse.BuildProperties
import mb.releng.eclipse.model.eclipse.Feature
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
    project.pluginManager.apply(EclipseBasePlugin::class)
    project.pluginManager.apply(MavenizeDslPlugin::class)
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    project.pluginManager.apply(BasePlugin::class)
    project.pluginManager.apply(MavenizePlugin::class)

    val log = GradleLog(project.logger)
    val mavenized = project.mavenizedEclipseInstallation()

    // Process feature.xml file.
    val featureXmlFile = project.file("feature.xml").toPath()
    if(Files.isRegularFile(featureXmlFile)) {
      val feature = Feature.read(featureXmlFile)
      if(project.name != feature.id) {
        log.warning("Project name ${project.name} and feature ID ${feature.id} do no match; feature JAR name will not match the feature ID")
      }
      if(project.version == Project.DEFAULT_VERSION) {
        // Set project version only if it it has not been set yet.
        project.version = feature.version.toMaven()
      }
      val converter = mavenized.createConverter(project.group.toString())
      val configuration = project.pluginConfiguration
      configuration.defaultDependencies {
        for(dependency in feature.dependencies) {
          val coords = converter.convert(dependency.coordinates)
          val isMavenizedBundle = mavenized.isMavenizedBundle(coords.groupId, coords.id)
          if(!isMavenizedBundle) {
            this.add(coords.toGradleDependency(project, configuration.name))
          }
        }
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

    // Run Eclipse with dependencies.
    val prepareEclipseRunConfigurationTask = project.tasks.create<PrepareEclipseRunConfig>("prepareRunConfiguration") {
      setFromMavenizedEclipseInstallation(mavenized)
      doFirst {
        for(file in project.pluginConfiguration) {
          addBundle(file)
        }
      }
    }
    project.tasks.create<EclipseRun>("run") {
      configure(prepareEclipseRunConfigurationTask, mavenized)
    }
  }
}
