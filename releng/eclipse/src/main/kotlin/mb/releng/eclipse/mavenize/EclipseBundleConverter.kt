package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Log
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarInputStream
import java.util.jar.Manifest

/**
 * Converts Eclipse bundle metadata from manifest files into Maven metadata. Since Eclipse bundles have no consistent
 * notion of group IDs, the given [groupId] is used.
 */
class EclipseBundleConverter(private val groupId: String) {
  /**
   * Converts Eclipse bundle at [bundleJar] to Maven metadata.
   */
  fun convertBundleJarFile(bundleJar: Path, log: Log): MavenMetadata {
    val manifest: Manifest
    when {
      !Files.exists(bundleJar) -> {
        throw IOException("Bundle file $bundleJar does not exist")
      }
      Files.isDirectory(bundleJar) -> {
        throw IOException("Bundle file $bundleJar is a directory")
      }
      else -> {
        manifest = Files.newInputStream(bundleJar).buffered().use {
          JarInputStream(it).manifest
        } ?: throw IOException("Could not get bundle manifest in JAR file $bundleJar")
      }
    }
    return convertManifest(manifest, log)
  }

  /**
   * Converts Eclipse manifest file [manifestFile] to Maven metadata.
   */
  fun convertManifestFile(manifestFile: Path, log: Log): MavenMetadata {
    val manifest = Files.newInputStream(manifestFile).buffered().use { inputStream ->
      Manifest(inputStream)
    }
    return convertManifest(manifest, log)
  }

  /**
   * Converts Eclipse [manifest] to Maven metadata.
   */
  fun convertManifest(manifest: Manifest, log: Log): MavenMetadata {
    val symbolicName = manifest.mainAttributes.getValue("Bundle-SymbolicName")
      ?: throw IOException("Cannot convert manifest, it does not have a Bundle-SymbolicName attribute")
    val artifactId = when {
      // Symbolic name can contain extra data such as: "org.eclipse.core.runtime; singleton:=true". Take everything before the ;.
      symbolicName.contains(';') -> symbolicName.split(';')[0]
      else -> symbolicName
    }.trim()

    val version = run {
      var versionStr = manifest.mainAttributes.getValue("Bundle-Version")
      if(versionStr == null) {
        Version.zero()
      } else {
        versionStr = versionStr.trim()
        val version = Version.parse(versionStr)
        if(version != null) {
          // Remove qualifier to fix version range matching.
          version.withoutQualifier()
        } else {
          val zero = Version.zero()
          log.warning("Cannot parse version string $versionStr, defaulting to version $zero")
          zero
        }
      }
    }

    val requireBundle = manifest.mainAttributes.getValue("Require-Bundle")
    val dependencies = if(requireBundle != null) {
      parseOuterRequireBundleString(requireBundle, log)
    } else {
      arrayListOf()
    }
    return MavenMetadata(groupId, artifactId, version.toString(), dependencies)
  }


  private fun parseOuterRequireBundleString(str: String, log: Log): ArrayList<MavenDependency> {
    // can't split on ',', because ',' also appears inside version ranges
    val dependencies = arrayListOf<MavenDependency>()
    if(str.isEmpty()) {
      return dependencies
    }
    var quoted = false
    var pos = 0
    for(i in 0 until str.length) {
      val char = str[i]
      when {
        char == '"' -> quoted = !quoted
        char == ',' && !quoted -> {
          val inner = str.substring(pos, i)
          val dependency = parseInnerRequireBundleString(inner, log)
          dependencies.add(dependency)
          pos = i + 1
        }
      }
    }
    if(quoted) {
      throw RequireBundleParseException("Failed to parse Require-Bundle string '$str': a quote was not closed")
    }
    if(pos < str.length) {
      val inner = str.substring(pos, str.length)
      val dependency = parseInnerRequireBundleString(inner, log)
      dependencies.add(dependency)
    }
    return dependencies
  }

  private fun parseInnerRequireBundleString(str: String, log: Log): MavenDependency {
    val elements = str.split(';')
    if(elements.isEmpty()) {
      throw RequireBundleParseException("Failed to parse part of Require-Bundle string '$str': it does not have a name element")
    }
    val artifactId = elements[0].trim()
    var version: String = VersionRange.allVersionsRange().toString()
    var optional = false
    for(element in elements.subList(1, elements.size)) {
      when {
        element.startsWith("bundle-version") -> {
          var versionStr = element.substring(element.indexOf('=') + 1)
          if(versionStr.startsWith('"')) {
            versionStr = versionStr.substring(1)
          }
          if(versionStr.endsWith('"')) {
            versionStr = versionStr.substring(0, versionStr.length - 1)
          }
          versionStr = versionStr.trim()
          val parsedVersion = Version.parse(versionStr)
          val parsedVersionRange = VersionRange.parse(versionStr)
          version = when {
            parsedVersionRange != null -> parsedVersionRange.toString()
            parsedVersion != null -> {
              // Convert exact functions to range from that version to infinity. Remove qualifier to fix range matching.
              VersionRange(true, parsedVersion.withoutQualifier(), null, false).toString()
            }
            else -> {
              val allVersionsRange = VersionRange.allVersionsRange()
              log.warning("Failed to parse version requirement $versionStr, defaulting to version requirement $allVersionsRange")
              allVersionsRange.toString()
            }
          }
        }
        element.startsWith("resolution") -> {
          val resolution = element.substring(element.indexOf('=') + 1).trim()
          if(resolution == "optional") {
            optional = true
          }
        }
        // TODO: do we need to parse the visibility attribute and use it to set a scope?
      }
    }
    return MavenDependency(groupId, artifactId, version, optional)
  }
}

class RequireBundleParseException(message: String) : Exception(message)
