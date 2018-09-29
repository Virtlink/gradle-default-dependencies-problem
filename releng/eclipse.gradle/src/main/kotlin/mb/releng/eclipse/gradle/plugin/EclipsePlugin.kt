package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.task.MavenizeTargetPlatform
import mb.releng.eclipse.mavenize.EclipseBundleConverter
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.file.Files
import java.nio.file.Paths

class EclipsePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply("base")
    project.pluginManager.apply("java")

    val groupId = "eclipse-photon"
    val mavenizeDir = Paths.get(System.getProperty("user.home"), ".mavenize")
    val repoDir = mavenizeDir.resolve("repo")

    val mavenizeTargetPlatformTask = project.tasks.create(
      "mavenizeTargetPlatform",
      MavenizeTargetPlatform::class.java,
      /**
       * Choose url from:
       * - Drops    : http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
       * - Releases : http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
       */
      "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip",
      groupId,
      Paths.get("eclipse", "plugins"),
      mavenizeDir.resolve("eclipse_archive_cache"),
      repoDir
    )
    project.tasks.getByPath("build").dependsOn(mavenizeTargetPlatformTask)

    // TODO: need to "fix" in mavenizer, as the versions in Eclipse bundles are all wrong.

    project.repositories.maven {
      it.setUrl(repoDir)
    }

    val manifestFile = project.file("META-INF/MANIFEST.MF").toPath()
    if(Files.isRegularFile(manifestFile)) {
      val eclipseBundleConverter = EclipseBundleConverter(groupId)
      val mavenMetadata = eclipseBundleConverter.convertManifestFile(manifestFile)
      for(dependency in mavenMetadata.dependencies) {
        val configuration = if(dependency.optional) "compileOnly" else "compile"
        project.dependencies.add(configuration, dependency.dependencyString)
      }
    } else {
      project.logger.warn("Project has no 'META-INF/MANIFEST.MF' file, cannot configure Eclipse plugin dependencies")
    }

    // TODO: scan build.properties and set resources

  }
}