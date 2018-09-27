package mb.releng.eclipse.util

import java.io.OutputStream
import java.net.URL

fun downloadFileFromUrl(url: URL, outputStream: OutputStream) {
  url.openStream().buffered().use { inputStream ->
    inputStream.copyTo(outputStream)
  }
}