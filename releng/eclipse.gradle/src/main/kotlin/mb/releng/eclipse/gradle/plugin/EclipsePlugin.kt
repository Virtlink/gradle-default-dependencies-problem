package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.GradleLog
import mb.releng.eclipse.mavenize.EclipseArchiveRetriever
import mb.releng.eclipse.mavenize.EclipseBundleConverter
import mb.releng.eclipse.mavenize.EclipseBundleInstaller
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.file.Files
import java.nio.file.Paths

class EclipsePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val log = GradleLog(project.logger)

    project.pluginManager.apply("java")

    val groupId = "eclipse-photon"
    val mavenizeDir = Paths.get(System.getProperty("user.home"), ".mavenize")
    val repoDir = mavenizeDir.resolve("repo")

    /**
     * Choose url from:
     * - Drops    : http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
     * - Releases : http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
     */
    val url = "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip"
    val eclipseArchiveRetriever = EclipseArchiveRetriever(url, mavenizeDir.resolve("eclipse_archive_cache"))
    val hasUnpacked = eclipseArchiveRetriever.getArchive(log)
    if(hasUnpacked) {
      EclipseBundleInstaller(repoDir, groupId).use {
        val pluginsDir = eclipseArchiveRetriever.unpackDir.resolve(Paths.get("eclipse", "plugins"))
        it.installAllFromDirectory(pluginsDir, log)
      }
    }

    project.repositories.maven { artifactRepository ->
      artifactRepository.setUrl(repoDir)
    }
    //project.repositories(closureOf)

    val manifestFile = project.file("META-INF/MANIFEST.MF").toPath()
    if(Files.isRegularFile(manifestFile)) {
      val eclipseBundleConverter = EclipseBundleConverter(groupId)
      val mavenMetadata = eclipseBundleConverter.convertManifestFile(manifestFile, log)
      for(dependency in mavenMetadata.dependencies) {
        val configuration = if(dependency.optional) "compileOnly" else "compile"
        project.dependencies.add(configuration, dependency.dependencyString)
      }
    } else {
      project.logger.warn("Project has no 'META-INF/MANIFEST.MF' file, cannot configure Eclipse plugin dependencies")
    }

//    val mavenizeTargetPlatformTask = project.tasks.create(
//      "mavenizeTargetPlatform",
//      MavenizeTargetPlatform::class.java,
//
//      "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip",
//      groupId,
//      Paths.get("eclipse", "plugins"),
//      mavenizeDir.resolve("eclipse_archive_cache"),
//      repoDir
//    )
//    //project.tasks.getByPath("jar").dependsOn(mavenizeTargetPlatformTask)
//
//    project.tasks.create("configureBundleDependencies") { task ->
//      task.dependsOn("mavenizeTargetPlatform")
//      task.doLast { task ->
//
//      }
//    }
//    project.tasks.getByName("build").dependsOn("configureBundleDependencies")
  }
}