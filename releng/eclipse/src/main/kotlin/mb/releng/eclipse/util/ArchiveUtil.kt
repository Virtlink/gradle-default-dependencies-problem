package mb.releng.eclipse.util

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/**
 * Unpacks archive from [inputStream] into [unpackDirectory], using [fileName] for a file format hint.
 */
fun unpack(inputStream: InputStream, fileName: String, unpackDirectory: Path) {
  when {
    fileName.endsWith(".tar.gz") -> {
      GzipCompressorInputStream(inputStream).buffered().use { compressorInputStream ->
        unarchive(compressorInputStream, unpackDirectory)
      }
    }
    fileName.endsWith(".dmg") -> {
      throw TODO("Cannot unpack $fileName, unpacking DMG files is not yet supported")
    }
    else -> {
      unarchive(inputStream, unpackDirectory)
    }
  }
}

private fun unarchive(inputStream: InputStream, unpackDirectory: Path) {
  ArchiveStreamFactory().createArchiveInputStream(inputStream).use { archiveInputStream ->
    while(true) {
      val entry = archiveInputStream.nextEntry ?: break
      val name = entry.name
      if(!archiveInputStream.canReadEntryData(entry)) {
        throw IOException("Cannot unpack entry $name, variant not supported")
      }
      val path = unpackDirectory.resolve(Paths.get(name))
      if(!path.startsWith(unpackDirectory)) {
        throw IOException("Cannot unpack entry $name, resulting path $path is not in the unpack directory $unpackDirectory")
      }
      if(entry.isDirectory) {
        Files.createDirectories(path)
      } else {
        Files.newOutputStream(path).buffered().use {
          archiveInputStream.copyTo(it)
          it.flush()
        }
      }
    }
  }
}

/**
 * Packs [directory] into [outputStream] as a JAR file.
 */
fun packJar(directory: Path, outputStream: OutputStream) {
  val manifestFile = directory.resolve("META-INF/MANIFEST.MF")
  val manifest = if(Files.exists(manifestFile) && Files.isRegularFile(manifestFile)) {
    Files.newInputStream(manifestFile).buffered().use {
      Manifest(it)
    }
  } else {
    val manifest = Manifest()
    manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
    manifest
  }
  JarOutputStream(outputStream, manifest).use { jarOutputStream ->
    val paths = Files.walk(directory)
    for(path in paths) {
      if(path == directory) {
        // Skip the root directory, as it would be stored as '/' in the JAR (Zip) file, which is not allowed.
        continue
      }
      // Files.walk returns absolute paths, so we need to relativize them here.
      val relativePath = directory.relativize(path)
      if(relativePath == Paths.get("META-INF", "MANIFEST.MF")) {
        // Skip the 'META-INF/MANIFEST.MF' file, since this is added by passing the manifest into JarOutputStream's
        // constructor. Adding this file again here would create a duplicate file, which results in an exception.
        continue
      }
      val isDirectory = Files.isDirectory(path)
      val name = run {
        // JAR (Zip) files are required to use '/' as a path separator.
        var name = relativePath.toString().replace("\\", "/")
        if(isDirectory && !name.endsWith("/")) {
          // JAR (Zip) files require directories to end with '/'.
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
          Files.newInputStream(path).use {
            it.copyTo(jarOutputStream)
          }
          jarOutputStream.closeEntry()
        }
      }
    }
    // Manually close paths stream to free up OS resources.
    paths.close()
    jarOutputStream.flush()
  }
}
