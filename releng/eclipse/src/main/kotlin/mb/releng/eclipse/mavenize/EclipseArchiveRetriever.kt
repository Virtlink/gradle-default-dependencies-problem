package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Log
import mb.releng.eclipse.util.deleteNonEmptyDirectory
import mb.releng.eclipse.util.downloadFileFromUrl
import mb.releng.eclipse.util.unpack
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

class EclipseArchiveRetriever(private val url: String, cacheDir: Path) {
  val archiveFile: Path
  val unpackDir: Path

  init {
    val filename = run {
      val index = url.lastIndexOf('/')
      if(index == -1) {
        error("Cannot retrieve Eclipse plugin bundles for URL $url, it has no filename")
      }
      url.substring(index + 1)
    }
    unpackDir = cacheDir.resolve(filename.replace('.', '_'))
    archiveFile = cacheDir.resolve(filename)
  }

  /**
   * Retrieves an Eclipse archive and unpacks it. Returns true if something was unpacked, false otherwise.
   */
  fun getArchive(log: Log): Boolean {
    val unpackDirExists = Files.isDirectory(unpackDir)
    val shouldUnpack = downloadFileFromUrl(URL(url), archiveFile, log) || !unpackDirExists
    if(shouldUnpack) {
      if(unpackDirExists) {
        deleteNonEmptyDirectory(unpackDir)
      }
      unpack(archiveFile, unpackDir, log)
    }
    return shouldUnpack
  }
}