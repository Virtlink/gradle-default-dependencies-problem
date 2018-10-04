package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Log

class EclipseBundleToMavenArtifact(private val groupId: String) : EclipseBundleToMaven {
  fun convert(bundle: Bundle, log: Log): MavenArtifact {
    if(bundle.fragmentHost != null) {
      log.warning("Converting single bundle; ignored ${bundle.name}'s fragment host ${bundle.fragmentHost.name}")
    }
    return toMavenArtifact(bundle)
  }

  fun convertAll(bundles: Iterable<Bundle>): Collection<MavenArtifact> {
    val fragmentEmulator = FragmentEmulator(bundles)
    return bundles
      .map { fragmentEmulator.emulateFor(it) }
      .map { bundle -> toMavenArtifact(bundle) }
      .toList()
  }


  private fun toMavenArtifact(bundle: Bundle): MavenArtifact {
    val coordinates = bundleToCoordinates(bundle, groupId)
    val dependencies = bundle.requiredBundles.map { convertRequiredBundle(it, groupId) }
    return MavenArtifact(coordinates, dependencies)
  }
}
