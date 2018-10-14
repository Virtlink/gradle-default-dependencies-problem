package mb.releng.eclipse.model.maven

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException
import org.apache.maven.artifact.versioning.VersionRange

data class MavenVersion(
  val version: DefaultArtifactVersion
) : MavenVersionOrRange() {
  companion object {
    fun parse(str: String): MavenVersion {
      return MavenVersion(DefaultArtifactVersion(str))
    }

    fun from(major: Int, minor: Int? = null, incremental: Int? = null, qualifier: String? = null): MavenVersion {
      val majorStr = major.toString()
      val minorStr = if(minor != null) ".$minor" else ""
      val incrementalStr = if(incremental != null) ".$incremental" else ""
      val qualifierStr = if(qualifier != null) "-$qualifier" else ""
      val str = "$majorStr$minorStr$incrementalStr$qualifierStr"
      return MavenVersion.parse(str)
    }

    fun zero() = from(0)
  }

  val major get(): Int = version.majorVersion
  val minor get(): Int? = version.minorVersion
  val incremental get(): Int? = version.incrementalVersion
  val qualifier get(): String? = version.qualifier

  // TODO: what exactly makes a Maven version a snapshot? Currently just checking if qualifier contains SNAPSHOT.
  fun isSnapshot() = version.qualifier.contains("SNAPSHOT")

  override fun toString() = version.toString()
}

data class MavenVersionRange(
  val version: VersionRange
) : MavenVersionOrRange() {
  companion object {
    fun parse(str: String): MavenVersionRange? {
      return try {
        MavenVersionRange(VersionRange.createFromVersionSpec(str))
      } catch(_: InvalidVersionSpecificationException) {
        null
      }
    }

    fun from(minInclusive: Boolean, minVersion: MavenVersion, maxVersion: MavenVersion? = null, maxInclusive: Boolean = false): MavenVersionRange {
      val str = "${if(minInclusive) "[" else "("}$minVersion,${maxVersion ?: ""}${if(maxInclusive) "]" else ")"}"
      return parse(str) ?: error("Failed to build Maven version range from string '$str'")
    }

    fun any() = MavenVersionRange.from(true, MavenVersion.zero())
  }

  override fun toString() = version.toString()
}

sealed class MavenVersionOrRange {
  companion object {
    fun parse(str: String): MavenVersionOrRange {
      return MavenVersionRange.parse(str) ?: MavenVersion.parse(str)
    }
  }
}
