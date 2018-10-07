package mb.releng.eclipse.mavenize

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

data class EclipseBuildProperties(
  val sourceDirs: Collection<String>,
  val outputDir: String?,
  val binaryIncludes: Collection<String>
) {
  companion object {
    fun read(propertiesFile: Path): EclipseBuildProperties {
      Files.newInputStream(propertiesFile).buffered().use { inputStream ->
        return read(inputStream)
      }
    }

    fun read(propertiesInputStream: InputStream): EclipseBuildProperties {
      val properties = Properties()
      properties.load(propertiesInputStream)
      return read(properties)
    }

    fun read(properties: Properties): EclipseBuildProperties {
      val sourceDirs = (properties.getProperty("source..") ?: "").split(',')
      val outputDir = properties.getProperty("output..")
      val binaryIncludes = (properties.getProperty("bin.includes") ?: "")
        .split(',')
        // Remove '.' include, since it makes no sense to include everything.
        // Remove 'META-INF/' include, since a manifest is already included in JAR files.
        .filter { it != "." && it != "META-INF/" }
      return EclipseBuildProperties(sourceDirs, outputDir, binaryIncludes)
    }


    fun eclipsePluginDefaults(): EclipseBuildProperties {
      return EclipseBuildProperties(listOf(), null, listOf("plugin.xml"))
    }

    fun eclipseFeatureDefaults(): EclipseBuildProperties {
      return EclipseBuildProperties(listOf(), null, listOf("feature.xml"))
    }
  }
}