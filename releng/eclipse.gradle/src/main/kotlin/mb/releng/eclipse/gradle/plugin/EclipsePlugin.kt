package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.plugin.internal.*
import mb.releng.eclipse.gradle.task.EclipseRun
import mb.releng.eclipse.gradle.task.PrepareEclipseRunConfig
import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.gradle.util.toGradleDependency
import mb.releng.eclipse.mavenize.toEclipse
import mb.releng.eclipse.model.eclipse.BuildProperties
import mb.releng.eclipse.model.eclipse.Bundle
import mb.releng.eclipse.model.eclipse.BundleDependency
import mb.releng.eclipse.model.maven.Coordinates
import mb.releng.eclipse.model.maven.MavenVersion
import mb.releng.eclipse.model.maven.MavenVersionOrRange
import mb.releng.eclipse.util.readManifestFromFile
import mb.releng.eclipse.util.toStringMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.language.jvm.tasks.ProcessResources
import java.nio.file.Files

class EclipsePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBase::class)
    project.pluginManager.apply(MavenizeDslPlugin::class)
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    val log = GradleLog(project.logger)
    val pluginConfiguration = project.pluginConfiguration
    val pluginCompileOnlyConfiguration = project.pluginCompileOnlyConfiguration

    project.pluginManager.apply(MavenizePlugin::class)
    val mavenized = project.mavenizedEclipseInstallation()

    project.pluginManager.apply(JavaLibraryPlugin::class)
    val jarTask = project.tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)

    // Process META-INF/MANIFEST.MF file, if any.
    val manifestFile = project.file("META-INF/MANIFEST.MF").toPath()
    val manifest = if(Files.isRegularFile(manifestFile)) {
      val manifest = readManifestFromFile(manifestFile)
      // Read into bundle, convert into Maven artifact metadata, and set project version and default dependencies.
      val bundle = Bundle.readFromManifest(manifest, log)
      val groupId = project.group.toString()
      val converter = mavenized.createConverter(groupId)
      converter.recordBundle(bundle, groupId)
      val mavenArtifact = converter.convert(bundle)
      // TODO: set project name from manifest in a Settings plugin, instead of just checking it?
      if(project.name != mavenArtifact.coordinates.id) {
        log.warning("Project name '${project.name}' differs from name in manifest '${mavenArtifact.coordinates.id}'")
      }
      // Set project version only if it it has not been set yet.
      if(project.version == Project.DEFAULT_VERSION) {
        project.version = mavenArtifact.coordinates.version
      }
      // Add default dependencies to plugin configuration.
      pluginConfiguration.defaultDependencies {
        for(dependency in mavenArtifact.dependencies) {
          val coords = dependency.coordinates
          val isMavenizedBundle = mavenized.isMavenizedBundle(coords.groupId, coords.id)
          // Don't add mavenized and optional dependencies, they go into `project.pluginCompileOnlyConfiguration`.
          if(isMavenizedBundle || dependency.optional) continue
          // Use null (default) configuration when the dependency is a mavenized bundle, as Maven artifacts have no configuration.
          val configuration = if(isMavenizedBundle) null else pluginConfiguration.name
          this.add(coords.toGradleDependency(project, configuration))
        }
      }
      // Add default dependencies to compile-only plugin configuration.
      pluginCompileOnlyConfiguration.defaultDependencies {
        for(dependency in mavenArtifact.dependencies) {
          val coords = dependency.coordinates
          val isMavenizedBundle = mavenized.isMavenizedBundle(coords.groupId, coords.id)
          // Add mavenized and optional bundles to `project.pluginCompileOnlyConfiguration`.
          if(isMavenizedBundle || dependency.optional) {
            // Use null (default) configuration when the dependency is a mavenized bundle, as Maven artifacts have no configuration.
            val configuration = if(isMavenizedBundle) null else pluginConfiguration.name
            this.add(coords.toGradleDependency(project, configuration))
          }
        }
      }
      manifest
    } else {
      null
    }

    // Make the Java plugin's configurations extend our plugin configuration, so that all dependencies from our
    // configurations are included in the Java plugin configurations. Doing this after scanning dependencies, because
    // this may resolve our configurations.
    project.configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME).extendsFrom(pluginConfiguration)
    project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(pluginCompileOnlyConfiguration)

    // Prepare manifest and set it in the JAR task.
    val prepareManifestTask = project.tasks.create("prepareManifestTask") {
      dependsOn(pluginConfiguration)
      dependsOn(pluginCompileOnlyConfiguration)
      doLast {
        if(manifest != null) {
          jarTask.manifest.attributes(manifest.mainAttributes.toStringMap())
        }
        if(project.version == Project.DEFAULT_VERSION) {
          error("Version has not been set, cannot prepare bundle manifest")
        }

        val attributesMap = mutableMapOf<String, String>()
        val bundleCoordinates = Coordinates(project.group.toString(), project.name, MavenVersion.parse(project.version.toString())).toEclipse()
        bundleCoordinates.writeToManifestAttributes(attributesMap)

        val requiredBundles = (pluginConfiguration.allDependencies + pluginCompileOnlyConfiguration.allDependencies).map {
          val versionStr = it.version
          val version = if(versionStr != null) {
            MavenVersionOrRange.parse(versionStr).toEclipse()
          } else {
            null
          }
          BundleDependency(it.name, version)
        }
        BundleDependency.writeToRequireBundleManifestAttributes(requiredBundles, attributesMap)

        jarTask.manifest.attributes(attributesMap)
      }
    }
    jarTask.dependsOn(prepareManifestTask)

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
      add(EclipseBase.pluginConfigurationName, jarTask)
    }

    // Run Eclipse with this plugin and its dependencies.
    val prepareEclipseRunConfigurationTask = project.tasks.create<PrepareEclipseRunConfig>("prepareRunConfiguration") {
      dependsOn(jarTask)
      dependsOn(pluginConfiguration)
      setFromMavenizedEclipseInstallation(mavenized)
      doFirst {
        addBundle(jarTask)
        for(file in pluginConfiguration) {
          addBundle(file)
        }
      }
    }
    project.tasks.create<EclipseRun>("run") {
      configure(prepareEclipseRunConfigurationTask, mavenized, project.mavenizeExtension())
    }
  }
}
