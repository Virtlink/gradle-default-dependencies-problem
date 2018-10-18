package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.plugin.internal.*
import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.gradle.util.toGradleDependency
import mb.releng.eclipse.model.eclipse.BuildProperties
import mb.releng.eclipse.model.eclipse.Bundle
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByName
import org.gradle.language.jvm.tasks.ProcessResources
import java.nio.file.Files

class EclipsePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBasePlugin::class)
    project.pluginManager.apply(MavenizeDslPlugin::class)
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    project.pluginManager.apply(MavenizePlugin::class)
    val mavenized = project.mavenizedEclipseInstallation()

    val log = GradleLog(project.logger)

    // Process META-INF/MANIFEST.MF file, if any.
    val manifestFile = project.file("META-INF/MANIFEST.MF").toPath()
    if(Files.isRegularFile(manifestFile)) {
      val bundle = Bundle.readFromManifestFile(manifestFile, log)
      val groupId = project.group.toString()
      val converter = mavenized.createConverter(groupId)
      converter.recordBundle(bundle, groupId)
      val mavenArtifact = converter.convert(bundle)
      if(project.version == Project.DEFAULT_VERSION) {
        // Set project version only if it it has not been set yet.
        project.version = mavenArtifact.coordinates.version
      }
      // Add default dependencies to plugin configuration.
      val pluginConfiguration = project.pluginConfiguration
      pluginConfiguration.defaultDependencies {
        for(dependency in mavenArtifact.dependencies) {
          if(dependency.optional) continue
          // Use null (default) configuration when the dependency is a mavenized bundle.
          val coords = dependency.coordinates
          val configuration = if(mavenized.isMavenizedBundle(coords.groupId, coords.id)) null else pluginConfiguration.name
          this.add(coords.toGradleDependency(project, configuration))
        }
      }
      // Add default dependencies to optional plugin configuration.
      val pluginOptionalConfiguration = project.pluginOptionalConfiguration
      pluginOptionalConfiguration.defaultDependencies {
        for(dependency in mavenArtifact.dependencies) {
          if(!dependency.optional) continue
          val coords = dependency.coordinates
          val configuration = if(mavenized.isMavenizedBundle(coords.groupId, coords.id)) null else pluginOptionalConfiguration.name
          this.add(coords.toGradleDependency(project, configuration))
        }
      }
    } else {
      error("Cannot configure Eclipse plugin; project $project has no 'META-INF/MANIFEST.MF' file")
    }

    // Apply Java plugin, after setting dependencies, because it apparently eagerly resolves configurations, freezing them.
    project.pluginManager.apply(JavaPlugin::class)
    val jarTask = project.tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)
    if(Files.isRegularFile(manifestFile)) {
      jarTask.manifest.from(manifestFile)
    }
    // Make the Java plugin's configurations extend our plugin configuration, so that all dependencies from our
    // configurations are included in the Java plugin configurations.
    project.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(project.pluginConfiguration)
    project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(project.pluginOptionalConfiguration)

    // Process build properties.
    val properties = run {
      val propertiesFile = project.file("build.properties").toPath()
      if(Files.isRegularFile(propertiesFile)) {
        BuildProperties.read(propertiesFile)
      } else {
        BuildProperties.eclipsePluginDefaults()
      }
    }
    project.configure<SourceSetContainer> {
      val mainSourceSet = getByName(SourceSet.MAIN_SOURCE_SET_NAME)
      mainSourceSet.java {
        if(!properties.sourceDirs.isEmpty()) {
          setSrcDirs(properties.sourceDirs)
        }
        if(properties.outputDir != null) {
          outputDir = project.file(properties.outputDir!!)
        }
      }
    }
    project.tasks.getByName<ProcessResources>(JavaPlugin.PROCESS_RESOURCES_TASK_NAME) {
      from(project.projectDir) {
        for(resource in properties.binaryIncludes) {
          include(resource)
        }
      }
    }
    project.artifacts {
      add(EclipseBasePlugin.pluginConfigurationName, jarTask)
    }
  }
}