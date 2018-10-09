package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.mavenize.toMavenVersion
import mb.releng.eclipse.model.BuildProperties
import mb.releng.eclipse.model.Feature
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import java.nio.file.Files

class EclipseFeature : Plugin<Project> {
  override fun apply(project: Project) {
    project.afterEvaluate { this.configure() }
  }

  private fun Project.configure() {
    // Use base plugin for hooking into 'assemble' lifecycle task.
    this.pluginManager.apply(BasePlugin::class)
    this.pluginManager.apply(EclipseBasePlugin::class)

    // Process META-INF/MANIFEST.MF file, if any.
    val featureXmlFile = this.file("feature.xml").toPath()
    if(Files.isRegularFile(featureXmlFile)) {
      val feature = Feature.read(featureXmlFile)
      // TODO: id is ignored, since we cannot change name of the project any more at this point.
      if(project.version == "unspecified") {
        project.version = feature.version.toMavenVersion()
      }
      for(dependency in feature.dependencies) {
        dependencies.add("plugin", "${project.group}:${dependency.id}:${dependency.version.toMavenVersion()}")
      }
    } else {
      error("Cannot configure Eclipse feature; project $this has no 'feature.xml' file")
    }

    // Process build properties.
    val properties = run {
      val propertiesFile = this.file("build.properties").toPath()
      if(Files.isRegularFile(propertiesFile)) {
        BuildProperties.read(propertiesFile)
      } else {
        BuildProperties.eclipseFeatureDefaults()
      }
    }
    val jarTask = this.tasks.create<Jar>("jar") {
      from(this@configure.projectDir) {
        for(resource in properties.binaryIncludes) {
          include(resource)
        }
      }
    }
    this.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(jarTask)
  }
}
