package mb.releng.eclipse.util

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URL

/**
 * Downloads file at given [url] into [outputStream]. Use a buffered [outputStream] for best performance.
 */
fun downloadFileFromUrl(url: URL, outputStream: OutputStream) {
  url.openStream().buffered().use { inputStream ->
    inputStream.copyTo(outputStream)
  }
}

/**
 * Downloads file at given [url] into memory (byte array).
 */
fun downloadFileFromUrl(url: URL): ByteArray {
  ByteArrayOutputStream().use {
    downloadFileFromUrl(url, it)
    return it.toByteArray()
  }
}
