package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Log

class EclipseBundleConverter(private val groupId: String) {
  fun convert(bundle: Bundle, log: Log): MavenArtifact {
    val coordinates = bundleToCoordinates(bundle)
    val dependencies = bundle.requiredBundles.map { convertRequiredBundle(it) }
    if(bundle.fragmentHost != null) {
      log.warning("Converting single bundle; ignored fragment host dependency from ${bundle.fragmentHost.name} to ${bundle.name}")
    }
    return MavenArtifact(coordinates, dependencies)
  }

  fun convertAll(bundles: Iterable<Bundle>): Collection<MavenArtifact> {
    // TODO: handle fragment host dependencies
  }

  fun convertToInstallable(bundle: BundleWithLocation): InstallableMavenArtifact {

  }

  fun convertAllToInstallable(bundles: Iterable<BundleWithLocation>): Collection<InstallableMavenArtifact> {
    // TODO: handle fragment host dependencies
  }


  private fun bundleToCoordinates(bundle: Bundle): Coordinates {
    val version = convertVersion(bundle.version)
    return Coordinates(groupId, bundle.name, version)
  }

  private fun convertRequiredBundle(requiredBundle: BundleDependency): MavenDependency {
    val version = convertDependencyVersion(requiredBundle.version)
    val coordinates = Coordinates(groupId, requiredBundle.name, version, null, null)
    // TODO: should we set scope with requiredBundle.visibility?
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

  private fun convertDependencyVersion(version: DependencyVersion?): String {
    return when(version) {
      is Version -> {
        // Bundle dependency versions mean version *or higher*, so we convert it into a version range from the version
        // to anything.
        convertVersionRange(VersionRange(true, version, null, false))
      }
      is VersionRange -> convertVersionRange(version)
      null -> {
        // No explicit bundle dependency version means *any version*, so we convert it into a version range from '0'
        // to anything.
        convertVersionRange(VersionRange(true, Version.zero(), null, false))
      }
    }
  }
}
