package mb.releng.eclipse.mavenize

import mb.releng.eclipse.model.*
import mb.releng.eclipse.util.Log
import mb.releng.eclipse.util.TempDir
import mb.releng.eclipse.util.packJar
import java.nio.file.Files
import java.nio.file.Path

class EclipseBundleToMavenArtifact(groupId: String) {
  private val converter = EclipseBundleConverter(groupId)


  fun convert(bundle: Bundle): MavenArtifact {
    converter.addBundle(bundle)
    return converter.bundles().map { converter.toMavenArtifact(it) }.first()
  }

  fun convertAll(bundles: Iterable<Bundle>): Collection<MavenArtifact> {
    converter.addBundles(bundles)
    return converter.bundles().map { bundle -> converter.toMavenArtifact(bundle) }
  }
}

class EclipseBundleToInstallableMavenArtifact(groupId: String, private val tempDir: TempDir) {
  private val converter = EclipseBundleConverter(groupId)
  private val bundleLocations = hashMapOf<String, Path>()


  fun convert(bundleWithLocation: BundleWithLocation, log: Log): InstallableMavenArtifact {
    val (bundle, location) = bundleWithLocation
    bundleLocations[bundle.name] = location
    converter.addBundle(bundle)
    return converter.bundles().map { toInstallableMavenArtifact(it, log) }.first()
  }

  fun convertAll(bundlesWithLocations: Iterable<BundleWithLocation>, log: Log): Collection<InstallableMavenArtifact> {
    bundlesWithLocations.forEach {
      val (bundle, location) = it
      bundleLocations[bundle.name] = location
      converter.addBundle(bundle)
    }
    return converter.bundles().filter {
      // Skip source bundles, since they are installed as sub-artifacts to the source bundle they belong to.
      it.sourceBundleFor == null
    }.map { toInstallableMavenArtifact(it, log) }
  }


  private fun toInstallableMavenArtifact(bundle: Bundle, log: Log): InstallableMavenArtifact {
    val location = bundleLocations[bundle.name]
      ?: error("Cannot convert bundle $bundle to an installable Maven artifact; it has no location")
    val jarFile = jarFileForLocation(location, log)
    val (coordinates, dependencies) = converter.toMavenArtifact(bundle)
    val primaryArtifact = PrimaryArtifact(coordinates, jarFile)
    val subArtifacts = mutableListOf<SubArtifact>()
    val pomFile = tempDir.createTempFile("${coordinates.id}-${coordinates.version}", ".pom")
    val pomSubArtifact = createPomSubArtifact(pomFile, coordinates, dependencies)
    subArtifacts.add(pomSubArtifact)
    val sourceBundle = converter.sourceBundle(bundle)
    if(sourceBundle != null) {
      val sourcesLocation = bundleLocations[sourceBundle.name]
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


internal class EclipseBundleConverter(private val groupId: String) {
  private val bundles = arrayListOf<Bundle>()
  private val fragmentHostToGuests = hashMapOf<String, HashSet<Bundle>>()
  private val sourceBundle = hashMapOf<String, Bundle>()


  fun addBundle(bundle: Bundle) {
    bundles.add(bundle)
    if(bundle.fragmentHost != null) {
      // HACK: ignore version of the fragment host; just look at the name.
      val toGuests = fragmentHostToGuests.getOrPut(bundle.fragmentHost.name) { hashSetOf() }
      toGuests.add(bundle)
    }
    if(bundle.sourceBundleFor != null) {
      // HACK: ignore version of the source bundle; just look at the name.
      sourceBundle[bundle.sourceBundleFor.name] = bundle
    }
  }

  fun addBundles(bundles: Iterable<Bundle>) {
    bundles.forEach { addBundle(it) }
  }


  fun toMavenArtifact(bundle: Bundle): MavenArtifact {
    val coordinates = bundleToCoordinates(bundle, groupId)
    val dependencies = run {
      val dependencies = bundle.requiredBundles.map { convertRequiredBundle(it, groupId) }
      val fragmentGuestDependencies = fragmentGuestDependencies(bundle)
      dependencies + fragmentGuestDependencies
    }
    return MavenArtifact(coordinates, dependencies)
  }


  fun bundles(): Collection<Bundle> = bundles

  fun fragmentGuestDependencies(bundle: Bundle): Collection<MavenDependency> {
    val fragmentGuests = fragmentHostToGuests[bundle.name]
    return fragmentGuests?.map {
      // Emulate a fragment by creating a dependency from the guest to the host, which is mandatory and reexported.
      val bundleDependency = BundleDependency(it.name, it.version, DependencyResolution.Mandatory, DependencyVisibility.Reexport)
      convertRequiredBundle(bundleDependency, groupId)
    } ?: listOf()
  }

  fun sourceBundle(bundle: Bundle): Bundle? {
    return sourceBundle[bundle.name]
  }
}

private fun bundleToCoordinates(bundle: Bundle, groupId: String): Coordinates {
  val version = convertVersion(bundle.version)
  val classifier = if(bundle.sourceBundleFor != null) "sources" else null
  return Coordinates(groupId, bundle.name, version, classifier, "jar")
}

private fun convertRequiredBundle(requiredBundle: BundleDependency, groupId: String): MavenDependency {
  val version = convertDependencyVersion(requiredBundle.versionOrRange)
  val coordinates = Coordinates(groupId, requiredBundle.name, version, null, null)
  // HACK: Ignore requiredBundle.visibility, since it cannot be supported by Maven, because it would require a scope
  // that IS included in the compile classpath, IS NOT transitively propagated in the compile classpath, BUT IS
  // transitively propagated in the runtime classpath. We need something like Gradle's 'api' and 'implementation'
  // dependency configurations for this.
  return MavenDependency(coordinates, null, requiredBundle.resolution == DependencyResolution.Optional)
}

private fun convertVersion(version: Version?): String {
  if(version == null) {
    // Default to the minimum version: '0', when no version could be parsed.
    return "0"
  }
  // Remove qualifier to fix version range matching in Maven and Gradle.
  @Suppress("NAME_SHADOWING") val version = version.withoutQualifier()
  // Convert to Maven format. Qualifier can be ignored as it has been removed.
  val major = version.major
  val minor = if(version.minor != null) ".${version.minor}" else ""
  val patch = if(version.micro != null) ".${version.micro}" else ""
  return "$major$minor$patch"
}

private fun convertVersionRange(versionRange: VersionRange): String {
  // Remove qualifier to fix version range matching in Maven and Gradle.
  // Other than that, version ranges match, so just convert to a string.
  return versionRange.withoutQualifiers().toString()
}

private fun convertDependencyVersion(versionOrRange: VersionOrRange?): String {
  return when(versionOrRange) {
    is Version -> {
      // Bundle dependency versions mean version *or higher*, so we convert it into a version range from the version
      // to anything.
      convertVersionRange(VersionRange(true, versionOrRange, null, false))
    }
    is VersionRange -> convertVersionRange(versionOrRange)
    null -> {
      // No explicit bundle dependency version means *any version*, so we convert it into a version range from '0'
      // to anything.
      convertVersionRange(VersionRange(true, Version.zero(), null, false))
    }
  }
}