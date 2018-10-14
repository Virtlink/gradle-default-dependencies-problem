package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.mavenize.toGradleDependencyNotation
import mb.releng.eclipse.model.eclipse.Site
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import java.nio.file.Files

class EclipseRepository : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBasePlugin::class)
    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    project.pluginManager.apply(BasePlugin::class)

    //val log = GradleLog(project.logger)
    val mavenized = project.mavenizedEclipseInstallation()

    // Process site.xml file.
    val siteXmlFile = project.file("site.xml").toPath()
    if(Files.isRegularFile(siteXmlFile)) {
      val site = Site.read(siteXmlFile)
      val converter = mavenized.createConverter(project.group.toString())
      for(dependency in site.dependencies) {
        val dependencyCoordinates = converter.convert(dependency)
        val depNotation = dependencyCoordinates.toGradleDependencyNotation()
        project.dependencies.add(EclipseBasePlugin.featureConfigurationName, depNotation)
      }
    } else {
      error("Cannot configure Eclipse repository; project $project has no 'site.xml' file")
    }

    // Build the repository.
    val targetDir = project.buildDir.resolve("repository")
    val unpackFeaturesTask = project.tasks.create<Copy>("unpackFeatures") {
      destinationDir = targetDir
      project.configurations.getByName(EclipseBasePlugin.featureConfigurationName).forEach {
        from(project.zipTree(it))
      }
    }
    // TODO: task that actually creates the repository
    val zipRepositoryTask = project.tasks.create<Zip>("zipRepository") {
      dependsOn(unpackFeaturesTask)
      from(targetDir)
    }
    project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(zipRepositoryTask)
    project.artifacts {
      add(EclipseBasePlugin.repositoryConfigurationName, zipRepositoryTask)
    }
  }
}