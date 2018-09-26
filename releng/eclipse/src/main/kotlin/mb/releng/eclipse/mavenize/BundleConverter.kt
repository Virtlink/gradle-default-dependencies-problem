package mb.releng.eclipse.mavenize

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarInputStream
import java.util.jar.Manifest

/**
 * Converts bundle metadata from manifest files into Maven metadata.
 */
class BundleConverter(private val groupId: String) {
  fun convert(bundleJar: Path): MavenMetadata {
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
        }
        if(manifest == null) {
          throw IOException("Could not get bundle manifest in JAR file $bundleJar")
        }
      }
    }
    return convert(manifest)
  }

  fun convert(manifest: Manifest): MavenMetadata {
    val symbolicName = manifest.mainAttributes.getValue("Bundle-SymbolicName")
    val artifactId = when {
      // Symbolic name can contain extra data such as: "org.eclipse.core.runtime; singleton:=true". Take everything before the ;.
      symbolicName.contains(';') -> symbolicName.split(';')[0]
      else -> symbolicName
    }.trim()
    val version = manifest.mainAttributes.getValue("Bundle-Version").replace(".qualifier", "-SNAPSHOT").trim()
    val requireBundle = manifest.mainAttributes.getValue("Require-Bundle")
    val dependencies = parseOuterRequireBundleString(requireBundle)
    return MavenMetadata(groupId, artifactId, version, dependencies)
  }


  private fun parseOuterRequireBundleString(str: String): ArrayList<MavenDependency> {
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
          val dependency = parseInnerRequireBundleString(inner)
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
      val dependency = parseInnerRequireBundleString(inner)
      dependencies.add(dependency)
    }
    return dependencies
  }

  private fun parseInnerRequireBundleString(str: String): MavenDependency {
    val elements = str.split(';')
    if(elements.isEmpty()) {
      throw RequireBundleParseException("Failed to parse part of Require-Bundle string '$str': it does not have a name element")
    }
    val artifactId = elements[0].trim()
    var version = "[0.1,)"
    var optional = false
    for(element in elements.subList(1, elements.size)) {
      when {
        element.startsWith("bundle-version") -> {
          version = element.substring(element.indexOf('=') + 1)
          if(version.startsWith('"')) {
            version = version.substring(1)
          }
          if(version.endsWith('"')) {
            version = version.substring(0, version.length - 1)
          }
          version = version.trim()
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
