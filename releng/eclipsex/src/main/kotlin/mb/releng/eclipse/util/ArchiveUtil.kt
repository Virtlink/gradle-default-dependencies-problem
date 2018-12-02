package mb.releng.eclipse.util

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/**
 * Unpacks archive from [archiveFile] into [unpackDirectory].
 */
fun unpack(archiveFile: Path, unpackDirectory: Path, log: Log) {
  val path = archiveFile.toString()
  Files.newInputStream(archiveFile).buffered().use { inputStream ->
    when {
      path.endsWith(".tar.gz") -> {
        GzipCompressorInputStream(inputStream).buffered().use { compressorInputStream ->
          unarchive(compressorInputStream, unpackDirectory, log)
        }
      }
      path.endsWith(".dmg") -> {
        throw TODO("Cannot unpack $path, unpacking DMG files is not yet supported")
      }
      else -> {
        unarchive(inputStream, unpackDirectory, log)
      }
    }
  }
}

private fun unarchive(inputStream: InputStream, unpackDirectory: Path, log: Log) {
  Files.createDirectories(unpackDirectory)
  ArchiveStreamFactory().createArchiveInputStream(inputStream).use { archiveInputStream ->
    while(true) {
      val entry = archiveInputStream.nextEntry ?: break
      val name = entry.name
      if(!archiveInputStream.canReadEntryData(entry)) {
        log.warning("Cannot unpack entry $name, format/variant not supported")
      }
      val path = unpackDirectory.resolve(Paths.get(name))
      if(!path.startsWith(unpackDirectory)) {
        throw IOException("Cannot unpack entry $name, resulting path $path is not in the unpack directory $unpackDirectory")
      }
      if(entry.isDirectory) {
        Files.createDirectories(path)
      } else {
        createParentDirectories(path)
        Files.newOutputStream(path).buffered().use {
          archiveInputStream.copyTo(it)
          it.flush()
        }
      }
    }
  }
}

/**
 * Packs [directory] into JAR file [jarFile].
 */
fun packJar(directory: Path, jarFile: Path) {
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
  createParentDirectories(jarFile)
  Files.newOutputStream(jarFile).buffered().use { outputStream ->
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
            Files.newInputStream(path).use { inputStream ->
              inputStream.copyTo(jarOutputStream)
            }
            jarOutputStream.closeEntry()
          }
        }
      }
      // Manually close paths stream to free up OS resources.
      paths.close()
      jarOutputStream.flush()
    }
    outputStream.flush()
  }
}
