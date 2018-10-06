package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.gradle.util.closureOf
import mb.releng.eclipse.mavenize.Bundle
import mb.releng.eclipse.mavenize.EclipseBundleToMavenArtifact
import mb.releng.eclipse.mavenize.Mavenizer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import org.gradle.api.internal.project.ProjectInternal
import java.nio.file.Files
import java.nio.file.Paths

class EclipsePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val log = GradleLog(project.logger)

    project.pluginManager.apply("java")

    // HACK: eagerly download and Mavenize bundles from Eclipse archive, as they must be available for dependency
    // resolution, which may or may not happen in the configuration phase. This costs at least one HTTP request per
    // configuration phase, to check if we need to download and Mavenize a new Eclipse archive.
    /**
     * Choose url from:
     * - Drops    : http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
     * - Releases : http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
     */
    val url = "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip"
    val groupId = "eclipse-photon"
    val mavenizeDir = Paths.get(System.getProperty("user.home"), ".mavenize")
    val mavenizer = Mavenizer(mavenizeDir, groupId, log)
    mavenizer.mavenize(url)
    val repoDir = mavenizer.repoDir

    // Add Mavenized repository to project repositories.
    project.repositories(closureOf<RepositoryHandler> {
      // HACK: get instance of BaseRepositoryFactory so that we can manually add a custom Maven repository.
      // From: https://discuss.gradle.org/t/how-can-i-get-hold-of-the-gradle-instance-of-the-repository-factory/6943/6
      val repositoryFactory = (project as ProjectInternal).services.get(BaseRepositoryFactory::class.java)
      val mavenRepo = repositoryFactory.createMavenRepository()
      mavenRepo.name = "mavenized"
      mavenRepo.setUrl(repoDir)
      // Add to top of repositories to speed up dependency resolution.
      addFirst(mavenRepo)
    })

    // Apply dependencies from MANIFEST.MF file, if any.
    val manifestFile = project.file("META-INF/MANIFEST.MF").toPath()
    if(Files.isRegularFile(manifestFile)) {
      val bundle = Bundle.readFromManifestFile(manifestFile, log)
      val converter = EclipseBundleToMavenArtifact(groupId)
      val mavenArtifact = converter.convert(bundle)

      for(dependency in mavenArtifact.dependencies) {
        val configuration = if(dependency.optional) "compileOnly" else "compile"
        project.dependencies.add(configuration, dependency.asGradleDependency)
      }
    } else {
      project.logger.warn("Project has no 'META-INF/MANIFEST.MF' file, cannot configure Eclipse plugin dependencies")
    }
  }
}