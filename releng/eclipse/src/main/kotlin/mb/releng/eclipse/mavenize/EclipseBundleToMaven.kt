package mb.releng.eclipse.mavenize

internal interface EclipseBundleToMaven {
  fun bundleToCoordinates(bundle: Bundle, groupId: String): Coordinates {
    val version = convertVersion(bundle.version)
    return Coordinates(groupId, bundle.name, version)
  }

  fun convertRequiredBundle(requiredBundle: BundleDependency, groupId: String): MavenDependency {
    val version = convertDependencyVersion(requiredBundle.version)
    val coordinates = Coordinates(groupId, requiredBundle.name, version, null, null)
    // HACK: Ignore requiredBundle.visibility, since it cannot be supported by Maven, because it would require a scope
    // that IS included in the compile classpath, IS NOT transitively propagated in the compile classpath, BUT IS
    // transitively propagated in the runtime classpath. We need something like Gradle's 'api' and 'implementation'
    // dependency configurations for this.
    return MavenDependency(coordinates, null, requiredBundle.resolution == DependencyResolution.Optional)
  }

  fun convertVersion(version: Version?): String {
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

  fun convertVersionRange(versionRange: VersionRange): String {
    // Remove qualifier to fix version range matching in Maven and Gradle.
    // Other than that, version ranges match, so just convert to a string.
    return versionRange.withoutQualifiers().toString()
  }

  fun convertDependencyVersion(version: DependencyVersion?): String {
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

internal class FragmentEmulator(bundles: Iterable<Bundle>) {
  private val fragmentHostToGuests = run {
    val map = hashMapOf<String, HashSet<Bundle>>()
    for(bundle in bundles) {
      if(bundle.fragmentHost != null) {
        // HACK: ignore versioning of fragment host; just look at the name.
        val toGuests = map.getOrPut(bundle.fragmentHost.name) { hashSetOf() }
        toGuests.add(bundle)
      }
    }
    map
  }

  fun emulateFor(bundle: Bundle): Bundle {
    val fragmentGuests = fragmentHostToGuests[bundle.name]
    return if(fragmentGuests != null) {
      // Emulate a fragment by creating a dependency from the guest to the host, which is mandatory and reexported.
      val fragmentGuestDependencies =
        fragmentGuests.map { BundleDependency(it.name, it.version, DependencyResolution.Mandatory, DependencyVisibility.Reexport) }
      Bundle(bundle.name, bundle.version, bundle.requiredBundles + fragmentGuestDependencies, bundle.fragmentHost)
    } else {
      bundle
    }
  }
}