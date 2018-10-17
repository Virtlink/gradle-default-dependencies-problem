package mb.releng.eclipse.mavenize

import mb.releng.eclipse.model.eclipse.*
import mb.releng.eclipse.model.maven.*


fun BundleVersion.toMaven(): MavenVersion {
  return MavenVersion.from(major, minor, micro, qualifier?.replace("qualifier", "SNAPSHOT"))
}

fun BundleVersionRange.toMaven(): MavenVersionRange {
  return MavenVersionRange.from(minInclusive, minVersion.toMaven(), maxVersion?.toMaven(), maxInclusive)
}

fun BundleVersionOrRange.toMaven(): MavenVersionOrRange {
  return when(this) {
    is BundleVersion -> {
      // Bundle dependency versions mean version *or higher*, so we convert it into a version range from the version to anything.
      MavenVersionRange.from(true, this.toMaven())
    }
    is BundleVersionRange -> toMaven()
  }
}


fun MavenVersion.toEclipse(): BundleVersion {
  return BundleVersion(major, minor, incremental, qualifier?.replace("SNAPSHOT", "qualifier"))
}

fun MavenVersionRange.toEclipse(): BundleVersionRange {
  return BundleVersionRange.parse(toString())
    ?: error("Could not convert Maven version range '$this' to an Eclipse bundle version range")
}

fun MavenVersionOrRange.toEclipse(): BundleVersionOrRange {
  return when(this) {
    is MavenVersion -> toEclipse()
    is MavenVersionRange -> toEclipse()
  }
}


fun BundleCoordinates.toMaven(groupId: String, classifier: String? = null, extension: String = "jar"): Coordinates {
  val version = version.toMaven()
  return Coordinates(groupId, name, version, classifier, extension)
}

fun Coordinates.toEclipse(): BundleCoordinates {
  val version = version.toEclipse()
  return BundleCoordinates(id, version)
}


fun BundleDependency.toMaven(
  groupId: String,
  classifier: String? = null,
  extension: String? = null,
  scope: String? = null
): MavenDependency {
  val version = version?.toMaven()
    ?: MavenVersionRange.any() // No explicit bundle dependency version means *any version*, so we convert it into a version range from '0' to anything.

  /**
   * TODO: use bundle dependency visibility, which cannot be supported by Maven, because it would require a scope
   * that IS included in the compile classpath, IS NOT transitively propagated in the compile classpath, BUT IS
   * transitively propagated in the runtime classpath. We need something like Gradle's 'api' and 'implementation'
   * dependency configurations for this.
   */
  val optional = resolution == DependencyResolution.Optional

  val coordinates = DependencyCoordinates(groupId, name, version, classifier, extension)
  return MavenDependency(coordinates, scope, optional)
}

fun MavenDependency.toEclipse(): BundleDependency {
  val version = coordinates.version.toEclipse()
  // TODO: convert scope to visibility, if possible (see TODO in BundleDependency.toMaven).
  val visibility = DependencyVisibility.default
  val resolution = if(optional) DependencyResolution.Optional else DependencyResolution.Mandatory
  return BundleDependency(coordinates.id, version, resolution, visibility)
}


fun Feature.Dependency.Coordinates.toMaven(
  groupId: String,
  classifier: String? = null,
  extension: String? = null
): DependencyCoordinates {
  val version = this.version.toMaven()
  return DependencyCoordinates(groupId, id, version, classifier, extension)
}

fun Site.Dependency.toMaven(
  groupId: String,
  classifier: String? = null,
  extension: String? = null
): DependencyCoordinates {
  val version = this.version.toMaven()
  return DependencyCoordinates(groupId, id, version, classifier, extension)
}