package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.downloadFileFromUrl
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * Retrieves an Eclipse archive, from [URL] constructed from [mirrorUrl]/[directory]/[filename], caching the result in
 * [cacheDirectory]. If an identical Eclipse archive exists in the cache directory (checked against the downloaded
 * sha512 file), nothing is downloaded. Always returns the path to the cached Eclipse archive file.
 */
fun retrieveEclipseArchive(mirrorUrl: String, directory: String, filename: String, cacheDirectory: Path): Path {
  val cachedShaFile = cacheDirectory.resolve("$filename.sha512")
  val shaUrl = URL("$mirrorUrl/$directory/$filename.sha512")
  val cachedFile = cacheDirectory.resolve(filename)
  val url = URL("$mirrorUrl/$directory/$filename")
  val download = if(Files.exists(cachedShaFile) && Files.isRegularFile(cachedShaFile) && Files.exists(cachedFile) && Files.isRegularFile(cachedFile)) {
    val shaBytes: ByteArray = Files.readAllBytes(cachedShaFile)
    val downloadedShaBytes = downloadFileFromUrl(shaUrl)
    !shaBytes.contentEquals(downloadedShaBytes)
  } else {
    true
  }
  if(download) {
    Files.createDirectories(cacheDirectory)
    Files.newOutputStream(cachedShaFile).use {
      downloadFileFromUrl(shaUrl, it)
      it.flush()
    }
    Files.newOutputStream(cachedFile).use {
      downloadFileFromUrl(url, it)
      it.flush()
    }
  }
  return cachedFile
}
