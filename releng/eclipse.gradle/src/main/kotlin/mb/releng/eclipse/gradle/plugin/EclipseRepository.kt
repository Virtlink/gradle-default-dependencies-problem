package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.util.toGradleDependency
import mb.releng.eclipse.model.eclipse.Site
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import java.nio.file.Files

open class EclipseRepositoryExtension(objects: ObjectFactory) {
  var repositoryDescriptionFile: String
    get() = _repositoryDescriptionFile.get()
    set(value) {
      _repositoryDescriptionFile.set(value)
    }

  private val _repositoryDescriptionFile: Property<String> = objects.property()
}

class EclipseRepository : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBasePlugin::class)
    project.pluginManager.apply(MavenizeDslPlugin::class)

    val extension = project.extensions.create<EclipseRepositoryExtension>("eclipseRepository", project.objects)
    extension.repositoryDescriptionFile = "site.xml"

    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    project.pluginManager.apply(BasePlugin::class)
    project.pluginManager.apply(MavenizePlugin::class)

    val mavenized = project.mavenizedEclipseInstallation()

    val extension = project.extensions.getByType<EclipseRepositoryExtension>()

    // Process repository description (site.xml) file.
    val repositoryDescriptionFile = project.file(extension.repositoryDescriptionFile).toPath()
    if(Files.isRegularFile(repositoryDescriptionFile)) {
      val site = Site.read(repositoryDescriptionFile)
      val converter = mavenized.createConverter(project.group.toString())
      val configuration = project.featureConfiguration
      configuration.defaultDependencies {
        for(dependency in site.dependencies) {
          val coords = converter.convert(dependency)
          this.add(coords.toGradleDependency(project, configuration.name))
        }
      }
    } else {
      error("Cannot configure Eclipse repository; project $project has no '$repositoryDescriptionFile' file")
    }

    // Build the repository.
    val unpackFeaturesDir = project.buildDir.resolve("unpackFeatures")
    val unpackFeaturesTask = project.tasks.create<Copy>("unpackFeatures") {
      destinationDir = unpackFeaturesDir
      project.configurations.getByName(EclipseBasePlugin.featureConfigurationName).forEach {
        from(project.zipTree(it))
      }
    }
    val repositoryDir = project.buildDir.resolve("repository")
    val eclipseLauncherPath = mavenized.launcherPath()?.toString() ?: error("Could not find Eclipse launcher")
    val createRepositoryTask = project.tasks.create("createRepository") {
      dependsOn(unpackFeaturesTask)
      inputs.file(eclipseLauncherPath)
      inputs.dir(unpackFeaturesDir)
      inputs.file(repositoryDescriptionFile)
      outputs.dir(repositoryDir)
      doLast {
        project.javaexec {
          main = "-jar"
          args = mutableListOf(
            eclipseLauncherPath,
            "-application", "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher",
            "-metadataRepository", "file:/$repositoryDir",
            "-artifactRepository", "file:/$repositoryDir",
            "-source", "$unpackFeaturesDir",
            "-configs", "ANY",
            "-compress",
            "-publishArtifacts"
          )
        }
        project.javaexec {
          main = "-jar"
          args = mutableListOf(
            eclipseLauncherPath,
            "-application", "org.eclipse.equinox.p2.publisher.CategoryPublisher",
            "-metadataRepository", "file:/$repositoryDir",
            "-categoryDefinition", "file:/$repositoryDescriptionFile",
            "-source", "$unpackFeaturesDir",
            "-categoryQualifier",
            "-compress"
          )
        }
      }
    }
    val zipRepositoryTask = project.tasks.create<Zip>("zipRepository") {
      dependsOn(createRepositoryTask)
      from(repositoryDir)
    }
    project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(zipRepositoryTask)
    project.artifacts {
      add(EclipseBasePlugin.repositoryConfigurationName, zipRepositoryTask)
    }
  }
}