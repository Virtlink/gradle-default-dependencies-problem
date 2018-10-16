package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.mavenize.toGradleDependencyNotation
import mb.releng.eclipse.model.eclipse.Site
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.project
import java.nio.file.Files

class EclipseRepository : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBasePlugin::class)
    project.pluginManager.apply(MavenizeDslPlugin::class)
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    project.pluginManager.apply(BasePlugin::class)
    project.pluginManager.apply(MavenizePlugin::class)

    val mavenized = project.mavenizedEclipseInstallation()

    // Process site.xml file.
    val siteXmlFile = project.file("site.xml").toPath()
    if(Files.isRegularFile(siteXmlFile)) {
      val site = Site.read(siteXmlFile)
      val converter = mavenized.createConverter(project.group.toString())
      for(dependency in site.dependencies) {
        val dependencyCoordinates = converter.convert(dependency)
        val depProjectPath = ":${dependencyCoordinates.id}"
        val depProject = project.findProject(depProjectPath)
        if(depProject != null) {
          project.dependencies.add(EclipseBasePlugin.featureConfigurationName, project.dependencies.project(depProjectPath, EclipseBasePlugin.featureConfigurationName))
        } else {
          val depNotation = dependencyCoordinates.toGradleDependencyNotation()
          project.dependencies.add(EclipseBasePlugin.featureConfigurationName, depNotation)
        }
      }
    } else {
      error("Cannot configure Eclipse repository; project $project has no 'site.xml' file")
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
    val createRepositoryTask = project.tasks.create<JavaExec>("createRepository") {
      dependsOn(unpackFeaturesTask)
      val launcherPath = mavenized.launcherPath()?.toString() ?: error("Could not find Eclipse launcher")
      inputs.file(launcherPath)
      inputs.dir(unpackFeaturesDir)
      outputs.dir(repositoryDir)
      main = "-jar"
      args = mutableListOf(
        launcherPath,
        "-application", "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher",
        "-metadataRepository", "file:/$repositoryDir",
        "-artifactRepository", "file:/$repositoryDir",
        "-source", "$unpackFeaturesDir",
        "-configs", "ANY",
        "-compress",
        "-publishArtifacts"
      )
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