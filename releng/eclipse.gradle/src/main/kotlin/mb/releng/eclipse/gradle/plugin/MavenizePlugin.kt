package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.mavenize.MavenizedEclipseInstallation
import mb.releng.eclipse.mavenize.mavenizeEclipseInstallation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

class MavenizePlugin : Plugin<Project> {
  companion object {
    const val mavenizedEclipseInstallationExtraName = "mavenized_eclipse_installation"
  }

  override fun apply(project: Project) {
    // HACK: eagerly download and Mavenize bundles from Eclipse archive, as they must be available for dependency
    // resolution, which may or may not happen in the configuration phase. This costs at least one HTTP request per
    // configuration phase, to check if we need to download and Mavenize a new Eclipse archive.
    val log = GradleLog(project.logger)
    val extension = project.mavenizeExtension()
    val mavenized = mavenizeEclipseInstallation(
      extension.mavenizeDir.get(),
      extension.url,
      extension.os.get().pluginsDir,
      extension.groupId.get(),
      log
    )
    project.extra.set(mavenizedEclipseInstallationExtraName, mavenized)
  }
}

fun Project.mavenizedEclipseInstallation(): MavenizedEclipseInstallation {
  if(!project.extra.has(MavenizePlugin.mavenizedEclipseInstallationExtraName)) {
    error("Tried to get Mavenized Eclipse installation, before MavenizePlugin was applied")
  }
  return project.extra[MavenizePlugin.mavenizedEclipseInstallationExtraName] as MavenizedEclipseInstallation
}
