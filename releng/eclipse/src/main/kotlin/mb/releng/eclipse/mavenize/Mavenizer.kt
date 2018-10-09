package mb.releng.eclipse.mavenize

import mb.releng.eclipse.model.BundleWithLocation
import mb.releng.eclipse.util.Log
import mb.releng.eclipse.util.TempDir
import java.nio.file.Path
import java.nio.file.Paths

class Mavenizer(private val mavenizeDir: Path, private val groupId: String, private val log: Log) {
  val repoDir = mavenizeDir.resolve("repo")!!

  private val installer = MavenArtifactInstaller(repoDir)

  fun mavenize(archiveUrl: String, forceDownload: Boolean = false, forceInstall: Boolean = false) {
    val archiveCacheDir = mavenizeDir.resolve("eclipse_archive_cache")
    val (unpackDir, wasUnpacked) = retrieveEclipseArchive(archiveUrl, archiveCacheDir, forceDownload, log)
    val eclipseBundlesPath = unpackDir.resolve(Paths.get("eclipse", "plugins"))
    if(wasUnpacked || forceInstall) {
      installer.delete(groupId)
      val bundles = BundleWithLocation.readAll(eclipseBundlesPath, log)
      TempDir("toInstallableMavenArtifacts").use { tempDir ->
        val converter = EclipseBundleToInstallableMavenArtifact(groupId, tempDir)
        val artifacts = converter.convertAll(bundles, log)
        installer.installAll(artifacts, log)
      }
    }
  }
}
