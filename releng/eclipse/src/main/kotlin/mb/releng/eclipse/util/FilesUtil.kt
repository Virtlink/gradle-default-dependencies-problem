package mb.releng.eclipse.util

import java.nio.file.Files
import java.nio.file.Path

fun deleteNonEmptyDirectory(directory: Path) {
  // Delete contents of temporary directory, and the directory itself.
  Files.walk(directory)
    .sorted(Comparator.reverseOrder())
    .forEach { Files.deleteIfExists(it) }
}