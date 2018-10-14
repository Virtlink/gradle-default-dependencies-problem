package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.mavenize.MavenizedEclipseInstallation
import mb.releng.eclipse.mavenize.mavenizeEclipseInstallation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import java.nio.file.Paths

class EclipseBasePlugin : Plugin<Project> {
  companion object {
    const val pluginConfigurationName = "eclipse-plugin"
    const val featureConfigurationName = "eclipse-feature"
    const val repositoryConfigurationName = "eclipse-repository"

    const val mavenizedEclipseInstallationExtraName = "mavenized_eclipse_installation"
  }

  override fun apply(project: Project) {
    project.configurations.create(pluginConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(featureConfigurationName) {
      setTransitive(false)
    }
    project.configurations.create(repositoryConfigurationName) {
      setTransitive(false)
    }

    val log = GradleLog(project.logger)

    // HACK: eagerly download and Mavenize bundles from Eclipse archive, as they must be available for dependency
    // resolution, which may or may not happen in the configuration phase. This costs at least one HTTP request per
    // configuration phase, to check if we need to download and Mavenize a new Eclipse archive.
    /**
     * Choose url from:
     * - Drops    : http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
     * - Releases : http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
     */
    val mavenizeUrl = "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip"
    val mavenizeGroupId = "eclipse-photon"
    val mavenizeDir = Paths.get(System.getProperty("user.home"), ".mavenize")
    val mavenized = mavenizeEclipseInstallation(mavenizeUrl, mavenizeDir, mavenizeGroupId, log)
    project.extra.set(mavenizedEclipseInstallationExtraName, mavenized)
  }
}

fun Project.mavenizedEclipseInstallation(): MavenizedEclipseInstallation {
  if(!project.extra.has(EclipseBasePlugin.mavenizedEclipseInstallationExtraName)) {
    error("Tried to get Mavenized Eclipse installation, before EclipseBasePlugin was applied")
  }
  return project.extra[EclipseBasePlugin.mavenizedEclipseInstallationExtraName] as MavenizedEclipseInstallation
}