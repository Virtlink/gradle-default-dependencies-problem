package mb.releng.eclipse.util

import java.nio.file.Files
import java.nio.file.Path

fun deleteNonEmptyDirectoryIfExists(directory: Path) {
  if(!Files.exists(directory)) return
  // Delete contents of temporary directory, and the directory itself.
  Files.walk(directory)
    .sorted(Comparator.reverseOrder())
    .forEach { Files.deleteIfExists(it) }
}

fun createParentDirectories(path: Path) {
  val parent = path.parent
  if(parent != null) {
    Files.createDirectories(parent)
  }
}
