package mb.releng.eclipse.util

import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

fun createJarFromDirectory(directory: Path, outputStream: OutputStream) {
  val manifest = Manifest()
  manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
  JarOutputStream(outputStream, manifest).use { jarOutputStream ->
    val paths = Files.walk(directory)
    for(path in paths) {
      val isDirectory = Files.isDirectory(path)
      val name = run {
        var name = path.toString().replace("\\", "/")
        if(isDirectory && !name.endsWith("/")) {
          name = "$name/"
        }
        name
      }
      val entry = JarEntry(name)
      entry.time = Files.getLastModifiedTime(path).toMillis()
      jarOutputStream.putNextEntry(entry)
      when {
        isDirectory -> {
          jarOutputStream.closeEntry()
        }
        else -> {
          jarOutputStream.write(Files.readAllBytes(path))
          jarOutputStream.closeEntry()
        }
      }
    }
    paths.close()
    jarOutputStream.flush()
  }
}
