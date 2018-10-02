package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Log
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.regex.Pattern
import java.util.stream.Collectors

data class BundleWithLocation(val bundle: Bundle, val location: Path) {
  companion object {
    fun read(jarFileOrDir: Path, log: Log): BundleWithLocation {
      val bundle = Bundle.read(jarFileOrDir, log)
      return BundleWithLocation(bundle, jarFileOrDir)
    }

    fun readAll(dir: Path, log: Log): Collection<BundleWithLocation> {
      val bundlePaths = Files.list(dir)
      val bundles = bundlePaths
        .map { read(it, log) }
        .collect(Collectors.toList())
      bundlePaths.close()
      return bundles
    }
  }
}

data class Bundle(
  val name: String,
  val version: Version?,
  val requiredBundles: Collection<BundleDependency>,
  val fragmentHost: BundleDependency?
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
      val symbolicName = manifest.mainAttributes.getValue("Bundle-SymbolicName")
        ?: throw IOException("Cannot read bundle from manifest, it does not have a Bundle-SymbolicName attribute")
      val name = when {
        // Symbolic name can contain extra data such as: "org.eclipse.core.runtime; singleton:=true". Take everything
        // before the ;.
        symbolicName.contains(';') -> symbolicName.split(';')[0]
        else -> symbolicName
      }.trim()

      val version = run {
        val versionStr = manifest.mainAttributes.getValue("Bundle-Version")
        if(versionStr == null) {
          Version.zero()
        } else {
          val version = Version.parse(versionStr.trim())
          if(version != null) {
            version
          } else {
            log.warning("Failed to parse version '$versionStr', defaulting to no version (matches minimum version)")
            null
          }
        }
      }

      val requireBundleStr = manifest.mainAttributes.getValue("Require-Bundle")
      val requiredBundles = if(requireBundleStr != null) {
        BundleDependency.parse(requireBundleStr, log)
      } else {
        arrayListOf()
      }

      val fragmentHostStr = manifest.mainAttributes.getValue("Fragment-Host")
      val fragmentHost = if(fragmentHostStr != null) {
        BundleDependency.parseInner(fragmentHostStr, log)
      } else {
        null
      }

      return Bundle(name, version, requiredBundles, fragmentHost)
    }
  }
}

data class BundleDependency(
  val name: String,
  val version: DependencyVersion?,
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
      var version: DependencyVersion? = null
      var resolution = DependencyResolution.Mandatory
      var visibility = DependencyVisibility.Private
      for(element in elements.subList(1, elements.size)) {
        @Suppress("NAME_SHADOWING") val element = element.trim()
        when {
          element.startsWith("bundle-version") -> {
            // Expected format: version="<str>", strip to <str>.
            version = DependencyVersion.parse(stripElement(element), log)
          }
          element.startsWith("resolution") -> {
            // Expected format: resolution:="<str>", strip to <str>.
            resolution = DependencyResolution.parse(stripElement(element))
          }
          element.startsWith("visibility") -> {
            // Expected format: visibility:="<str>", strip to <str>.
            visibility = DependencyVisibility.parse(stripElement(element))
          }
          // TODO: do we need to parse the visibility attribute and use it to set a scope?
        }
      }
      return BundleDependency(name, version, resolution, visibility)
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

sealed class DependencyVersion {
  companion object {
    internal fun parse(str: String, log: Log): DependencyVersion? {
      val parsedVersion = Version.parse(str)
      if(parsedVersion != null) {
        return parsedVersion
      }
      val parsedVersionRange = VersionRange.parse(str)
      if(parsedVersionRange != null) {
        return parsedVersionRange
      }
      log.warning("Failed to parse dependency version '$str', defaulting to no version (matches any version)")
      return null
    }
  }
}

enum class DependencyResolution {
  Mandatory,
  Optional;

  companion object {
    internal fun parse(str: String): DependencyResolution {

      return when(str) {
        "mandatory" -> Mandatory
        "optional" -> Optional
        else -> Mandatory
      }
    }

    internal fun toString(resolution: DependencyResolution): String {
      return when(resolution) {
        DependencyResolution.Mandatory -> "mandatory"
        DependencyResolution.Optional -> "optional"
      }
    }
  }
}

enum class DependencyVisibility {
  Private,
  Reexport;

  companion object {
    internal fun parse(str: String): DependencyVisibility {

      return when(str) {
        "private" -> Private
        "reexport" -> Reexport
        else -> Private
      }
    }

    internal fun toString(visibility: DependencyVisibility): String {
      return when(visibility) {
        DependencyVisibility.Private -> "private"
        DependencyVisibility.Reexport -> "reexport"
      }
    }
  }
}

data class Version(
  val major: Int,
  val minor: Int?,
  val micro: Int?,
  val qualifier: String?
) : DependencyVersion() {
  companion object {
    private val pattern = Pattern.compile("""(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:\.(.+))?""")

    fun parse(str: String): Version? {
      val matcher = pattern.matcher(str)
      if(!matcher.matches()) return null
      val major = matcher.group(1)?.toInt() ?: return null
      val minor = matcher.group(2)?.toInt()
      val micro = matcher.group(3)?.toInt()
      val qualifier = matcher.group(4)
      return Version(major, minor, micro, qualifier)
    }

    fun zero() = Version(0, null, null, null)
  }

  fun withoutQualifier() = Version(major, minor, micro, null)

  override fun toString() =
    "$major${if(minor != null) ".$minor" else ""}${if(micro != null) ".$micro" else ""}${if(qualifier != null) ".$qualifier" else ""}"
}

data class VersionRange(
  val minInclusive: Boolean,
  val minVersion: Version,
  val maxVersion: Version?,
  val maxInclusive: Boolean
) : DependencyVersion() {
  companion object {
    private val pattern = Pattern.compile("""([\[\(])(.+)\w*,\w*(.*)([\]\)])""")

    fun parse(str: String): VersionRange? {
      val matcher = pattern.matcher(str)
      if(!matcher.matches()) return null
      val minChr = matcher.group(1) ?: return null
      val minVerStr = matcher.group(2) ?: return null
      val minVer = Version.parse(minVerStr) ?: return null
      val maxVerStr = matcher.group(3)
      val maxVer = Version.parse(maxVerStr)
      val maxChr = matcher.group(4) ?: return null
      return VersionRange(minChr == "[", minVer, maxVer, maxChr == "]")
    }

    fun anyVersionsRange() = VersionRange(true, Version.zero(), null, false)
  }

  fun withoutQualifiers() =
    VersionRange(minInclusive, minVersion.withoutQualifier(), maxVersion?.withoutQualifier(), maxInclusive)

  override fun toString(): String {
    return "${if(minInclusive) "[" else "("}$minVersion,${maxVersion ?: ""}${if(maxInclusive) "]" else ")"}"
  }
}
