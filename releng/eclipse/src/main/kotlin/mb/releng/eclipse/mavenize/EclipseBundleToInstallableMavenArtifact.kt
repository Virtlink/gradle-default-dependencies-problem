package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Log
import mb.releng.eclipse.util.TempDir
import mb.releng.eclipse.util.packJar
import java.nio.file.Files

class EclipseBundleToInstallableMavenArtifact(private val groupId: String, private val tempDir: TempDir) : EclipseBundleToMaven {
  fun convert(bundleWithLocation: BundleWithLocation, log: Log): InstallableMavenArtifact {
    val (bundle, _) = bundleWithLocation
    if(bundle.fragmentHost != null) {
      log.warning("Converting single bundle; ignored ${bundle.name}'s fragment host ${bundle.fragmentHost.name}")
    }
    return toInstallableMavenArtifact(bundleWithLocation, log)
  }

  fun convertAll(bundles: Iterable<BundleWithLocation>, log: Log): Collection<InstallableMavenArtifact> {
    val fragmentEmulator = FragmentEmulator(bundles.map { it.bundle })
    return bundles
      .map {
        val emulatedBundle = fragmentEmulator.emulateFor(it.bundle)
        BundleWithLocation(emulatedBundle, it.location)
      }
      .map { toInstallableMavenArtifact(it, log) }
      .toList()
  }


  private fun toInstallableMavenArtifact(bundleWithLocation: BundleWithLocation, log: Log): InstallableMavenArtifact {
    val (bundle, location) = bundleWithLocation
    val jarFile = if(Files.isDirectory(location)) {
      val jarFile = tempDir.createTempFile(location.fileName.toString(), ".jar")
      log.debug("Packing bundle directory $location into JAR file $jarFile in preparation for Maven installation")
      packJar(location, jarFile)
      jarFile
    } else {
      location
    }
    val coordinates = bundleToCoordinates(bundle, groupId).withExtension("jar")
    val dependencies = bundle.requiredBundles.map { convertRequiredBundle(it, groupId) }
    val primaryArtifact = PrimaryArtifact(coordinates, jarFile)
    val pomFile = tempDir.createTempFile("${coordinates.id}-${coordinates.version}", ".pom")
    val pomSubArtifact = createPomSubArtifact(pomFile, coordinates, dependencies)
    return InstallableMavenArtifact(primaryArtifact, listOf(pomSubArtifact), dependencies)
  }
}