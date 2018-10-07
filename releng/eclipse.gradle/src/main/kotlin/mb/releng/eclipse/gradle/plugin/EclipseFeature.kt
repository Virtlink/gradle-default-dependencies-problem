package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.mavenize.EclipseBuildProperties
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

    // Process build properties.
    val properties = run {
      val propertiesFile = this.file("build.properties").toPath()
      if(Files.isRegularFile(propertiesFile)) {
        EclipseBuildProperties.read(propertiesFile)
      } else {
        EclipseBuildProperties.eclipseFeatureDefaults()
      }
    }
    val jarTask = this.tasks.create<Jar>("jar") {
      from(this@configure.projectDir) {
        for(resource in properties.binaryIncludes) {
          include(resource)
        }
      }
    }
    this.tasks.getByName("assemble").dependsOn(jarTask)
  }
}
