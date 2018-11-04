package mb.releng.eclipse.util

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.Manifest

fun readManifestFromFile(file: Path): Manifest {
  return Files.newInputStream(file).buffered().use { inputStream ->
    Manifest(inputStream)
  }
}

fun Attributes.toStringMap(): Map<String, String> {
  return entries.map { it.key.toString() to it.value.toString() }.toMap()
}
