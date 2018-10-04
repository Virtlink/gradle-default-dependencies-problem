package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * Retrieves an Eclipse archive and unpacks it.
 */
fun retrieveEclipseArchive(urlStr: String, cacheDir: Path, forceDownload: Boolean, log: Log): EclipseArchive {
  val filename = run {
    val index = urlStr.lastIndexOf('/')
    if(index == -1) {
      error("Cannot retrieve Eclipse plugin bundles for URL $urlStr, it has no filename")
    }
    urlStr.substring(index + 1)
  }
  val unpackDir = cacheDir.resolve(filename.replace('.', '_'))
  val archiveFile = cacheDir.resolve(filename)

  val url = URL(urlStr)
  val shouldDownload = forceDownload || shouldDownload(url, archiveFile)
  if(shouldDownload) {
    log.progress("Downloading $url into $archiveFile")
    downloadFileFromUrl(url, archiveFile)
  }
  val unpackDirExists = Files.isDirectory(unpackDir)
  val shouldUnpack = shouldDownload || !unpackDirExists
  if(shouldUnpack) {
    if(unpackDirExists) {
      deleteNonEmptyDirectoryIfExists(unpackDir)
    }
    log.progress("Unpacking $archiveFile into $unpackDir")
    unpack(archiveFile, unpackDir, log)
  }
  return EclipseArchive(unpackDir, shouldUnpack)
}

data class EclipseArchive(val unpackDir: Path, val wasUnpacked: Boolean)