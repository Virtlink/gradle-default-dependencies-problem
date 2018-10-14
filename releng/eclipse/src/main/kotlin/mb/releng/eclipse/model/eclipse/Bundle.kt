package mb.releng.eclipse.model.eclipse

import mb.releng.eclipse.util.Log
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.stream.Collectors

data class BundleCoordinates(val name: String, val version: BundleVersion) {
  override fun toString() = "$name@$version"
}

data class Bundle(
  val coordinates: BundleCoordinates,
  val requiredBundles: Collection<BundleDependency>,
  val fragmentHost: BundleDependency?,
  val sourceBundleFor: BundleDependency?
) {
  companion object {
    fun read(jarFileOrDir: Path, log: Log): Bundle {
      val manifest: Manifest = when {
        !Files.exists(jarFileOrDir) -> {
          throw IOException("Bundle file or directory $jarFileOrDir does not exist")
        }
        Files.isDirectory(jarFileOrDir) -> {
          val manifestFile = jarFileOrDir.resolve(Paths.get("META-INF", "MANIFEST.MF"))
          Files.newInputStream(manifestFile).buffered().use { inputStream ->
            Manifest(inputStream)
          }
        }
        else -> {
          Files.newInputStream(jarFileOrDir).buffered().use { inputStream ->
            JarInputStream(inputStream).manifest
          } ?: throw IOException("Could not get bundle manifest in JAR file $jarFileOrDir")
        }
      }
      return readFromManifest(manifest, log)
    }

    fun readFromManifestFile(manifestFile: Path, log: Log): Bundle {
      val manifest = Files.newInputStream(manifestFile).buffered().use { inputStream ->
        Manifest(inputStream)
      }
      return readFromManifest(manifest, log)
    }

    fun readFromManifest(manifest: Manifest, log: Log): Bundle {
      val name = run {
        val symbolicName = manifest.mainAttributes.getValue("Bundle-SymbolicName")
          ?: throw BundleParseException("Cannot read bundle from manifest, it does not have a Bundle-SymbolicName attribute")
        when {
          // Symbolic name can contain extra data such as: "org.eclipse.core.runtime; singleton:=true". Take everything
          // before the ;.
          symbolicName.contains(';') -> symbolicName.split(';')[0]
          else -> symbolicName
        }.trim()
      }

      val version = run {
        val versionStr = manifest.mainAttributes.getValue("Bundle-Version")?.trim()
        if(versionStr == null) {
          throw BundleParseException("Cannot read bundle '$name' from manifest, it does not have a Bundle-Version attribute")
        } else {
          BundleVersion.parse(versionStr)
            ?: throw BundleParseException("Cannot read bundle '$name' from manifest, failed to parse Bundle-Version '$versionStr'")
        }
      }

      val coordinates = BundleCoordinates(name, version)

      val requireBundleStr = manifest.mainAttributes.getValue("Require-Bundle")
      val requiredBundles = if(requireBundleStr != null) {
        try {
          BundleDependency.parse(requireBundleStr, log)
        } catch(e: RequireBundleParseException) {
          throw BundleParseException("Cannot read bundle '$name' from manifest, failed to parse Require-Bundle '$requireBundleStr'", e)
        }
      } else {
        arrayListOf()
      }

      val fragmentHostStr = manifest.mainAttributes.getValue("Fragment-Host")
      val fragmentHost = if(fragmentHostStr != null) {
        try {
          BundleDependency.parseInner(fragmentHostStr, log)
        } catch(e: RequireBundleParseException) {
          throw BundleParseException("Cannot read bundle '$name' from manifest, failed to parse Fragment-Host '$fragmentHostStr'", e)
        }
      } else {
        null
      }

      val sourceBundleStr = manifest.mainAttributes.getValue("Eclipse-SourceBundle")
      val sourceBundleFor = if(sourceBundleStr != null) {
        try {
          BundleDependency.parseInner(sourceBundleStr, log)
        } catch(e: RequireBundleParseException) {
          throw BundleParseException("Cannot read bundle '$name' from manifest, failed to parse Eclipse-SourceBundle '$sourceBundleStr'", e)
        }
      } else {
        null
      }

      return Bundle(coordinates, requiredBundles, fragmentHost, sourceBundleFor)
    }
  }

  override fun toString() = coordinates.toString()
}

class BundleParseException(message: String, cause: Throwable?) : Exception(message, cause) {
  constructor(message: String) : this(message, null)
}

data class BundleDependency(
  val name: String,
  val version: BundleVersionOrRange?,
  val resolution: DependencyResolution,
  val visibility: DependencyVisibility
) {
  companion object {
    internal fun parse(str: String, log: Log): Collection<BundleDependency> {
      // Can't split on ',', because it also appears inside quoted version ranges. Manually parse to handle quotes.
      val requiredBundles = mutableListOf<BundleDependency>()
      if(str.isEmpty()) {
        return requiredBundles
      }
      var quoted = false
      var pos = 0
      for(i in 0 until str.length) {
        val char = str[i]
        when {
          char == '"' -> quoted = !quoted
          char == ',' && !quoted -> {
            val inner = str.substring(pos, i)
            val dependency = parseInner(inner, log)
            requiredBundles.add(dependency)
            pos = i + 1
          }
        }
      }
      if(quoted) {
        throw RequireBundleParseException("Failed to parse Require-Bundle string '$str': a quote was not closed")
      }
      if(pos < str.length) {
        val inner = str.substring(pos, str.length)
        val dependency = parseInner(inner, log)
        requiredBundles.add(dependency)
      }
      return requiredBundles
    }

    internal fun parseInner(str: String, log: Log): BundleDependency {
      val elements = str.split(';')
      if(elements.isEmpty()) {
        throw RequireBundleParseException("Failed to parse part of Require-Bundle string '$str': it does not have a name element")
      }
      val name = elements[0].trim()
      var versionOrRange: BundleVersionOrRange? = null
      var resolution = DependencyResolution.Mandatory
      var visibility = DependencyVisibility.Private
      for(element in elements.subList(1, elements.size)) {
        @Suppress("NAME_SHADOWING") val element = element.trim()
        when {
          // HACK: support parsing Eclipse-SourceBundle by accepting 'version' elements.
          element.startsWith("bundle-version") || element.startsWith("version") -> {
            // Expected format: version="<str>", strip to <str>.
            versionOrRange = BundleVersionOrRange.parse(stripElement(element), log)
          }
          element.startsWith("resolution") -> {
            // Expected format: resolution:="<str>", strip to <str>.
            resolution = DependencyResolution.parse(stripElement(element))
          }
          element.startsWith("visibility") -> {
            // Expected format: visibility:="<str>", strip to <str>.
            visibility = DependencyVisibility.parse(stripElement(element))
          }
        }
      }
      return BundleDependency(name, versionOrRange, resolution, visibility)
    }

    private fun stripElement(element: String): String {
      @Suppress("NAME_SHADOWING") var element = element.substring(element.indexOf('=') + 1)
      if(element.startsWith('"')) {
        element = element.substring(1)
      }
      if(element.endsWith('"')) {
        element = element.substring(0, element.length - 1)
      }
      return element.trim()
    }
  }

  override fun toString(): String {
    val versionStr = if(version != null) {
      "bundle-version=\"$version\","
    } else {
      ""
    }
    val resolutionStr = "resolution:=${DependencyResolution.toString(resolution)}"
    val visibilityStr = ",visibility:=${DependencyVisibility.toString(visibility)}"
    return "$name;$versionStr$resolutionStr$visibilityStr"
  }
}

class RequireBundleParseException(message: String) : Exception(message)

enum class DependencyResolution {
  Mandatory,
  Optional;

  companion object {
    fun parse(str: String) = when(str) {
      "mandatory" -> Mandatory
      "optional" -> Optional
      else -> Mandatory
    }

    val default get() = Mandatory

    fun toString(resolution: DependencyResolution) = when(resolution) {
      Mandatory -> "mandatory"
      Optional -> "optional"
    }
  }
}

enum class DependencyVisibility {
  Private,
  Reexport;

  companion object {
    fun parse(str: String) = when(str) {
      "private" -> Private
      "reexport" -> Reexport
      else -> Private
    }

    val default get() = Private

    fun toString(visibility: DependencyVisibility) = when(visibility) {
      Private -> "private"
      Reexport -> "reexport"
    }
  }
}

