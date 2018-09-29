package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Logger
import mb.releng.eclipse.util.deleteNonEmptyDirectory
import mb.releng.eclipse.util.downloadFileFromUrl
import mb.releng.eclipse.util.unpack
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * Retrieves the path to Eclipse bundles by:
 *
 * - Using [cacheDirectory] as a directory to cache downloaded and unpacked archives.
 * - Downloading the archive from [prefix]/[filenameWithoutExtension].[extension] into
 *   [cacheDirectory]/[filenameWithoutExtension].[extension], if necessary.
 * - Unpacking the archive into [cacheDirectory]/[filenameWithoutExtension], if necessary.
 * - Returning [cacheDirectory]/[filenameWithoutExtension]/[pluginPathInArchive].
 */
fun retrieveEclipsePluginBundlesFromArchive(
  prefix: String,
  filenameWithoutExtension: String,
  extension: String,
  pluginPathInArchive: Path,
  cacheDirectory: Path,
  logger: Logger
): PluginBundlesResult {
  val cachedUnpackDirectory = cacheDirectory.resolve(filenameWithoutExtension)
  val cachedUnpackDirectoryExists = Files.isDirectory(cachedUnpackDirectory)
  val cachedFile = cacheDirectory.resolve("$filenameWithoutExtension.$extension")
  val shouldUnpack = downloadFileFromUrl(URL("$prefix/$filenameWithoutExtension.$extension"), cachedFile, logger) || !cachedUnpackDirectoryExists
  if(shouldUnpack) {
    if(cachedUnpackDirectoryExists) {
      deleteNonEmptyDirectory(cachedUnpackDirectory)
    }
    unpack(cachedFile, cachedUnpackDirectory, logger)
  }
  val pluginPath = cachedUnpackDirectory.resolve(pluginPathInArchive)
  return PluginBundlesResult(pluginPath, shouldUnpack)
}

data class PluginBundlesResult(val pluginPath: Path, val hasUnpacked: Boolean)
