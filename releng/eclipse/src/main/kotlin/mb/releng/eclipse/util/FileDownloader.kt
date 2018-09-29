package mb.releng.eclipse.util

import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * Downloads file at given [url] into [file]. Skips download if file at [url] and [file] have the same size.
 */
fun downloadFileFromUrl(url: URL, file: Path, logger: Logger): Boolean {
  if(Files.isDirectory(file)) {
    throw IOException("Cannot download file (from $url) into $file, as it is a directory")
  }
  val connection = url.openConnection()
  val download = if(!Files.exists(file)) {
    val parent = file.parent
    if(parent != null) {
      Files.createDirectories(parent)
    }
    true
  } else {
    val remoteContentLength = try {
      connection.getHeaderField("Content-Length")?.toLong()
    } catch(_: NumberFormatException) {
      null
    }
    // TODO: also check last modified date?
    remoteContentLength != null && remoteContentLength != Files.size(file)
  }
  if(!download) {
    logger.progress("Skipping download of $url, as file $file has the same size")
    return false
  }
  logger.progress("Downloading $url into $file")
  Files.newOutputStream(file).buffered().use { outputStream ->
    connection.getInputStream().buffered().use { inputStream ->
      inputStream.copyTo(outputStream)
    }
    outputStream.flush()
  }
  return true
}
