package mb.releng.eclipse.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipsePluginPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    println("Hello, Gradle!")
  }
}