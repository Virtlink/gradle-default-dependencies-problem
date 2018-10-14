package mb.releng.eclipse.mavenize

import mb.releng.eclipse.model.eclipse.Bundle
import mb.releng.eclipse.model.maven.InstallableMavenArtifact
import mb.releng.eclipse.model.maven.PrimaryArtifact
import mb.releng.eclipse.model.maven.SubArtifact
import mb.releng.eclipse.model.maven.createPomSubArtifact
import mb.releng.eclipse.util.Log
import mb.releng.eclipse.util.TempDir
import mb.releng.eclipse.util.packJar
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

data class MavenInstallableBundle(
  val bundle: Bundle,
  val groupId: String,
  val location: Path
) {
  companion object {
    fun read(jarFileOrDir: Path, groupId: String, log: Log): MavenInstallableBundle {
      val bundle = Bundle.read(jarFileOrDir, log)
      return MavenInstallableBundle(bundle, groupId, jarFileOrDir)
    }

    fun readAll(dir: Path, groupId: String, log: Log): Collection<MavenInstallableBundle> {
      val bundlePaths = Files.list(dir)
      val bundles = bundlePaths.map { read(it, groupId, log) }.collect(Collectors.toList())
      bundlePaths.close()
      return bundles
    }
  }
}

class EclipseBundleToInstallableMavenArtifact(private val tempDir: TempDir, private val fallbackGroupId: String) {
  fun convertAll(installableBundles: Iterable<MavenInstallableBundle>, log: Log): Collection<InstallableMavenArtifact> {
    val converter = EclipseConverter(fallbackGroupId)
    val bundleLocations = hashMapOf<String, Path>()
    installableBundles.forEach {
      val (bundle, groupId, location) = it
      converter.recordBundle(bundle, groupId)
      bundleLocations[bundle.coordinates.name] = location
    }
    return installableBundles.map {
      it.bundle
    }.filter {
      // Skip source bundles, since they are installed as sub-artifacts to the source bundle they belong to.
      it.sourceBundleFor == null
    }.map {
      toInstallableMavenArtifact(it, bundleLocations, converter, log)
    }
  }


  private fun toInstallableMavenArtifact(
    bundle: Bundle,
    bundleLocations: Map<String, Path>,
    converter: EclipseConverter,
    log: Log
  ): InstallableMavenArtifact {
    val location = bundleLocations[bundle.coordinates.name]
      ?: error("Cannot convert bundle $bundle to an installable Maven artifact; it has no location")
    val jarFile = jarFileForLocation(location, log)
    val (coordinates, dependencies) = converter.convert(bundle)
    val primaryArtifact = PrimaryArtifact(coordinates, jarFile)
    val subArtifacts = mutableListOf<SubArtifact>()
    val pomFile = tempDir.createTempFile("${coordinates.id}-${coordinates.version}", ".pom")
    val pomSubArtifact = createPomSubArtifact(pomFile, coordinates, dependencies)
    subArtifacts.add(pomSubArtifact)
    val sourceBundle = converter.sourceBundleFor(bundle)
    if(sourceBundle != null) {
      val sourcesLocation = bundleLocations[sourceBundle.coordinates.name]
        ?: error("Cannot convert source bundle $sourceBundle to a Maven sub-artifact; it has no location")
      val sourcesJarFile = jarFileForLocation(sourcesLocation, log)
      val sourcesSubArtifact = SubArtifact("sources", "jar", sourcesJarFile)
      subArtifacts.add(sourcesSubArtifact)
    }
    return InstallableMavenArtifact(primaryArtifact, subArtifacts, dependencies)
  }

  private fun jarFileForLocation(location: Path, log: Log): Path {
    return if(Files.isDirectory(location)) {
      val jarFile = tempDir.createTempFile(location.fileName.toString(), ".jar")
      log.debug("Packing bundle directory $location into JAR file $jarFile in preparation for Maven installation")
      packJar(location, jarFile)
      jarFile
    } else {
      location
    }
  }
}