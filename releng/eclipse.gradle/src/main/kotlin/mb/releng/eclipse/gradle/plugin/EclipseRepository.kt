package mb.releng.eclipse.gradle.plugin

import mb.releng.eclipse.gradle.plugin.internal.*
import mb.releng.eclipse.gradle.task.EclipseRun
import mb.releng.eclipse.gradle.task.PrepareEclipseRunConfig
import mb.releng.eclipse.gradle.util.GradleLog
import mb.releng.eclipse.gradle.util.toGradleDependency
import mb.releng.eclipse.model.eclipse.Site
import mb.releng.eclipse.util.TempDir
import mb.releng.eclipse.util.packJar
import mb.releng.eclipse.util.unpack
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

open class EclipseRepositoryExtension(objects: ObjectFactory) {
  var repositoryDescriptionFile: String
    get() = _repositoryDescriptionFile.get()
    set(value) {
      _repositoryDescriptionFile.set(value)
    }

  var qualifierReplacement: String
    get() = _qualifierReplacement.get()
    set(value) {
      _qualifierReplacement.set(value)
    }

  private val _repositoryDescriptionFile: Property<String> = objects.property()
  private val _qualifierReplacement: Property<String> = objects.property()
}

class EclipseRepository : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(EclipseBasePlugin::class)
    project.pluginManager.apply(MavenizeDslPlugin::class)

    val extension = project.extensions.create<EclipseRepositoryExtension>("eclipseRepository", project.objects)
    extension.repositoryDescriptionFile = "site.xml"
    extension.qualifierReplacement = SimpleDateFormat("yyyyMMddHHmm").format(Calendar.getInstance().time)

    project.afterEvaluate { configure(this) }
  }

  private fun configure(project: Project) {
    val log = GradleLog(project.logger)

    val extension = project.extensions.getByType<EclipseRepositoryExtension>()

    project.pluginManager.apply(BasePlugin::class)

    project.pluginManager.apply(MavenizePlugin::class)
    val mavenized = project.mavenizedEclipseInstallation()

    // Process repository description (site.xml) file.
    val repositoryDescriptionFile = project.file(extension.repositoryDescriptionFile).toPath()
    if(Files.isRegularFile(repositoryDescriptionFile)) {
      val site = Site.read(repositoryDescriptionFile)
      val converter = mavenized.createConverter(project.group.toString())
      val configuration = project.featureConfiguration
      configuration.defaultDependencies {
        for(dependency in site.dependencies) {
          val coords = converter.convert(dependency)
          this.add(coords.toGradleDependency(project, configuration.name))
        }
      }
    } else {
      error("Cannot configure Eclipse repository; project $project has no '$repositoryDescriptionFile' file")
    }

    // Unpack dependency features.
    val unpackFeaturesDir = project.buildDir.resolve("unpackFeatures")
    val unpackFeaturesTask = project.tasks.create<Copy>("unpackFeatures") {
      destinationDir = unpackFeaturesDir
      project.featureConfiguration.forEach {
        from(project.zipTree(it))
      }
    }

    // Replace '.qualifier' with concrete qualifiers in all features and plugins. Have to do the unpacking/packing of
    // JAR files manually, as we cannot create Gradle tasks during execution.
    val featuresInUnpackFeaturesDir = unpackFeaturesDir.resolve("features").toPath()
    val pluginsInUnpackFeaturesDir = unpackFeaturesDir.resolve("plugins").toPath()
    val concreteQualifier = extension.qualifierReplacement
    val replaceQualifierDir = project.buildDir.resolve("replaceQualifier")
    val replaceQualifierTask = project.tasks.create("replaceQualifier") {
      dependsOn(unpackFeaturesTask)
      inputs.dir(unpackFeaturesDir)
      outputs.dir(replaceQualifierDir)
      doLast {
        TempDir("replaceQualifier").use { tempDir ->
          val replaceQualifierFeaturesDir = replaceQualifierDir.resolve("features").toPath()
          Files.list(featuresInUnpackFeaturesDir).use { featureJarFiles ->
            for(featureJarFile in featureJarFiles) {
              val fileName = featureJarFile.fileName
              val unpackTempDir = tempDir.createTempDir(fileName.toString())
              unpack(featureJarFile, unpackTempDir, log)
              val featureFile = unpackTempDir.resolve("feature.xml")
              if(Files.isRegularFile(featureFile)) {
                // TODO: this could have false positives, do a model 2 model transformation instead?
                featureFile.replaceInFile(".qualifier", ".$concreteQualifier")
              } else {
                log.warning("Unable to replace qualifiers in versions for $fileName, as it has no feature.xml file")
              }
              val targetJarFile = replaceQualifierFeaturesDir.resolve(fileName)
              packJar(unpackTempDir, targetJarFile)
            }
          }
          val replaceQualifierPluginsDir = replaceQualifierDir.resolve("plugins").toPath()
          Files.list(pluginsInUnpackFeaturesDir).use { pluginJarFiles ->
            for(pluginJarFile in pluginJarFiles) {
              val fileName = pluginJarFile.fileName
              val unpackTempDir = tempDir.createTempDir(fileName.toString())
              unpack(pluginJarFile, unpackTempDir, log)
              val manifestFile = unpackTempDir.resolve("META-INF/MANIFEST.MF")
              if(Files.isRegularFile(manifestFile)) {
                // TODO: this could have false positives, do a model 2 model transformation instead?
                manifestFile.replaceInFile(".qualifier", ".$concreteQualifier")
              } else {
                log.warning("Unable to replace qualifiers in versions for $fileName, as it has no META-INF/MANIFEST.MF file")
              }
              val targetJarFile = replaceQualifierPluginsDir.resolve(fileName)
              packJar(unpackTempDir, targetJarFile)
            }
          }
        }
      }
    }

    // Build the repository.
    val repositoryDir = project.buildDir.resolve("repository")
    val eclipseLauncherPath = mavenized.equinoxLauncherPath()?.toString() ?: error("Could not find Eclipse launcher")
    val createRepositoryTask = project.tasks.create("createRepository") {
      dependsOn(replaceQualifierTask)
      inputs.dir(replaceQualifierDir)
      inputs.file(eclipseLauncherPath)
      inputs.file(repositoryDescriptionFile)
      outputs.dir(repositoryDir)
      doLast {
        project.javaexec {
          main = "-jar"
          args = mutableListOf(
            eclipseLauncherPath,
            "-application", "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher",
            "-metadataRepository", "file:/$repositoryDir",
            "-artifactRepository", "file:/$repositoryDir",
            "-source", "$replaceQualifierDir",
            "-configs", "ANY",
            "-compress",
            "-publishArtifacts"
          )
        }
        project.javaexec {
          main = "-jar"
          args = mutableListOf(
            eclipseLauncherPath,
            "-application", "org.eclipse.equinox.p2.publisher.CategoryPublisher",
            "-metadataRepository", "file:/$repositoryDir",
            "-categoryDefinition", "file:/$repositoryDescriptionFile",
            "-categoryQualifier",
            "-compress"
          )
        }
      }
    }

    // Zip the repository.
    val zipRepositoryTask = project.tasks.create<Zip>("zipRepository") {
      dependsOn(createRepositoryTask)
      from(repositoryDir)
    }
    project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(zipRepositoryTask)
    project.artifacts {
      add(EclipseBasePlugin.repositoryConfigurationName, zipRepositoryTask)
    }

    // Run Eclipse with unpacked plugins.
    val prepareEclipseRunConfigurationTask = project.tasks.create<PrepareEclipseRunConfig>("prepareRunConfiguration") {
      dependsOn(unpackFeaturesTask)
      setFromMavenizedEclipseInstallation(mavenized)
      doFirst {
        Files.list(pluginsInUnpackFeaturesDir).use { pluginFiles ->
          for(file in pluginFiles) {
            addBundle(file)
          }
        }
      }
    }
    project.tasks.create<EclipseRun>("run") {
      configure(prepareEclipseRunConfigurationTask, mavenized)
    }
  }
}

private fun Path.replaceInFile(pattern: String, replacement: String) {
  // TODO: more efficient way to replace strings in a file?
  val text = String(Files.readAllBytes(this)).replace(pattern, replacement)
  Files.newOutputStream(this).buffered().use { outputStream ->
    PrintStream(outputStream).use {
      it.print(text)
      it.flush()
    }
    outputStream.flush()
  }
}
