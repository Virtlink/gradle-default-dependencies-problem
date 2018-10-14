package mb.releng.eclipse.model.eclipse

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

data class BuildProperties(
  val sourceDirs: Collection<String>,
  val outputDir: String?,
  val binaryIncludes: Collection<String>
) {
  companion object {
    fun read(propertiesFile: Path): BuildProperties {
      Files.newInputStream(propertiesFile).buffered().use { inputStream ->
        return read(inputStream)
      }
    }

    fun read(propertiesInputStream: InputStream): BuildProperties {
      val properties = Properties()
      properties.load(propertiesInputStream)
      return read(properties)
    }

    fun read(properties: Properties): BuildProperties {
      val sourceDirs = (properties.getProperty("source..") ?: "").split(',')
      val outputDir = properties.getProperty("output..")
      val binaryIncludes = (properties.getProperty("bin.includes") ?: "")
        .split(',')
        // Remove '.' include, since it makes no sense to include everything.
        // Remove 'META-INF/' include, since a manifest is already included in JAR files.
        .filter { it != "." && it != "META-INF/" }
      return BuildProperties(sourceDirs, outputDir, binaryIncludes)
    }


    fun eclipsePluginDefaults(): BuildProperties {
      return BuildProperties(listOf(), null, listOf("plugin.xml"))
    }

    fun eclipseFeatureDefaults(): BuildProperties {
      return BuildProperties(listOf(), null, listOf("feature.xml"))
    }
  }
}