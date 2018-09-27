package mb.releng.eclipse.util

import java.io.InputStream
import java.nio.file.Path

fun unzip(inputStream: InputStream, unpackDirectory: Path) {
  TODO("Unpack zip file. See https://wiki.sei.cmu.edu/confluence/display/java/IDS04-J.+Safely+extract+files+from+ZipInputStream")
}

fun ungzip(inputStream: InputStream, unpackDirectory: Path) {
  TODO("Unpack gz file.")
}

fun untar(inputStream: InputStream, unpackDirectory: Path) {
  TODO("Unpack tar file.")
}
