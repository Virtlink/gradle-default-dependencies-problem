package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.gradle.util.closureOf
import mb.releng.eclipse.mavenize.toGradleDependencyNotation
import mb.releng.eclipse.model.eclipse.BuildProperties
import mb.releng.eclipse.model.eclipse.Bundle
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import org.gradle.api.internal.project.ProjectInternal
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
    project.pluginManager.apply(JavaPlugin::class)
    project.pluginManager.apply(MavenizePlugin::class)

    val log = GradleLog(project.logger)
    val mavenized = project.mavenizedEclipseInstallation()

    // Add Mavenized repository to project repositories.
    // HACK: get instance of BaseRepositoryFactory so that we can manually add a custom Maven repository.
    // From: https://discuss.gradle.org/t/how-can-i-get-hold-of-the-gradle-instance-of-the-repository-factory/6943/6
    project.run {
      val repositoryFactory = (this as ProjectInternal).services.get(BaseRepositoryFactory::class.java)
      this.repositories(closureOf<RepositoryHandler> {
        val mavenRepo = repositoryFactory.createMavenRepository()
        mavenRepo.name = "mavenized"
        mavenRepo.setUrl(mavenized.repoDir)
        // Add to top of repositories to speed up dependency resolution.
        addFirst(mavenRepo)
      })
    }

    val jarTask = project.tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)

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
      for(dependency in mavenArtifact.dependencies) {
        val configuration = if(dependency.optional) {
          JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
        } else {
          JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
        }
        project.dependencies.add(configuration, dependency.coordinates.toGradleDependencyNotation())
      }
      jarTask.manifest.from(manifestFile)
    } else {
      error("Cannot configure Eclipse plugin; project $project has no 'META-INF/MANIFEST.MF' file")
    }

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