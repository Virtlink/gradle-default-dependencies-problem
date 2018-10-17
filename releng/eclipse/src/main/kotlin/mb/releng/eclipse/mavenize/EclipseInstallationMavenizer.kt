package mb.releng.eclipse.mavenize

import mb.releng.eclipse.model.eclipse.*
import mb.releng.eclipse.util.Log
import mb.releng.eclipse.util.TempDir
import mb.releng.eclipse.util.deleteNonEmptyDirectoryIfExists
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

fun mavenizeEclipseInstallation(
  mavenizeDir: Path,
  installationArchiveUrl: String,
  installationPluginsDirRelative: Path,
  groupId: String,
  log: Log,
  forceDownload: Boolean = false,
  forceInstall: Boolean = false
): MavenizedEclipseInstallation {
  // Setup paths.
  val repoDir = mavenizeDir.resolve("repo")!!
  val repoGroupIdDir = repoDir.resolve(groupId)!!
  val archiveCacheDir = mavenizeDir.resolve("eclipse_archive_cache")

  // Retrieve an Eclipse installation.
  val (installationDir, wasUnpacked) = retrieveEclipseInstallation(installationArchiveUrl, archiveCacheDir, forceDownload, log)
  val installationPluginsDir = installationDir.resolve(installationPluginsDirRelative)

  // Install bundles if needed.
  if(wasUnpacked || forceInstall) {
    // Delete repository entries for the group ID.
    deleteNonEmptyDirectoryIfExists(repoGroupIdDir)

    // Read bundles and pre-process them.
    val bundles = MavenInstallableBundle.readAll(installationPluginsDir, groupId, log).map { installableBundle ->
      val (bundle, bundleGroupId, location) = installableBundle

      // Remove qualifiers to fix version range matching in Maven and Gradle.
      val newCoordinates = bundle.coordinates.run {
        BundleCoordinates(name, version.withoutQualifier())
      }

      fun BundleVersionOrRange?.fixDepVersion() = when(this) {
        null -> null
        is BundleVersion -> withoutQualifier()
        is BundleVersionRange -> withoutQualifiers()
      }

      fun BundleDependency.fixDep() = BundleDependency(name, version.fixDepVersion(), resolution, visibility)

      val newDeps = bundle.requiredBundles.map { it.fixDep() }
      val newFragmentHost = bundle.fragmentHost?.fixDep()
      val newSourceBundleFor = bundle.sourceBundleFor?.fixDep()
      val newBundle = Bundle(newCoordinates, newDeps, newFragmentHost, newSourceBundleFor)
      MavenInstallableBundle(newBundle, bundleGroupId, location)
    }

    // Convert Eclipse bundles to Maven artifacts and install them.
    TempDir("toInstallableMavenArtifacts").use { tempDir ->
      val converter = EclipseBundleToInstallableMavenArtifact(tempDir, groupId)
      val artifacts = converter.convertAll(bundles, log)
      val installer = MavenArtifactInstaller(repoDir)
      installer.installAll(artifacts, log)
    }
  }

  // Collect names of Eclipse installation bundles by directory listing of all installed artifacts.
  val installedBundleDirs = Files.list(repoGroupIdDir)
  val installationBundleNames = installedBundleDirs.map { it.fileName.toString() }.collect(Collectors.toSet())
  installedBundleDirs.close()
  return MavenizedEclipseInstallation(groupId, repoDir, installationDir, installationPluginsDir, installationBundleNames)
}

data class MavenizedEclipseInstallation(
  val groupId: String,
  val repoDir: Path,
  val installationDir: Path,
  val installationPluginsDir: Path,
  val installationBundleNames: Set<String>
) {
  fun isMavenizedBundle(groupId: String, id: String) = groupId == this.groupId && installationBundleNames.contains(id)

  fun createConverter(fallbackGroupId: String): EclipseConverter {
    val converter = EclipseConverter(fallbackGroupId)
    for(bundleName in installationBundleNames) {
      converter.recordGroupId(bundleName, groupId)
    }
    return converter
  }

  fun launcherPath(): Path? {
    val matcher = FileSystems.getDefault().getPathMatcher("glob:org.eclipse.equinox.launcher_*.jar")
    val plugins = Files.list(installationPluginsDir)
    val launcher = plugins.filter {
      matcher.matches(it.fileName)
    }.findFirst()
    return if(!launcher.isPresent) {
      null
    } else {
      launcher.get()
    }
  }
}
