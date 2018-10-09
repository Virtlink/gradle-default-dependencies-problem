package mb.releng.eclipse.model

import mb.releng.eclipse.util.Log
import java.util.regex.Pattern

data class Version(
  val major: Int,
  val minor: Int?,
  val micro: Int?,
  val qualifier: String?
) : VersionOrRange() {
  companion object {
    private val pattern = Pattern.compile("""(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:\.(.+))?""")

    fun parse(str: String): Version? {
      val matcher = pattern.matcher(str)
      if(!matcher.matches()) return null
      val major = matcher.group(1)?.toInt() ?: return null
      val minor = matcher.group(2)?.toInt()
      val micro = matcher.group(3)?.toInt()
      val qualifier = matcher.group(4)
      return Version(major, minor, micro, qualifier)
    }

    fun zero() = Version(0, null, null, null)
  }

  fun withoutQualifier() = Version(major, minor, micro, null)

  override fun toString() =
    "$major${if(minor != null) ".$minor" else ""}${if(micro != null) ".$micro" else ""}${if(qualifier != null) ".$qualifier" else ""}"
}

data class VersionRange(
  val minInclusive: Boolean,
  val minVersion: Version,
  val maxVersion: Version?,
  val maxInclusive: Boolean
) : VersionOrRange() {
  companion object {
    private val pattern = Pattern.compile("""([\[\(])(.+)\w*,\w*(.*)([\]\)])""")

    fun parse(str: String): VersionRange? {
      val matcher = pattern.matcher(str)
      if(!matcher.matches()) return null
      val minChr = matcher.group(1) ?: return null
      val minVerStr = matcher.group(2) ?: return null
      val minVer = Version.parse(minVerStr) ?: return null
      val maxVerStr = matcher.group(3)
      val maxVer = Version.parse(maxVerStr)
      val maxChr = matcher.group(4) ?: return null
      return VersionRange(minChr == "[", minVer, maxVer, maxChr == "]")
    }

    fun anyVersionsRange() = VersionRange(true, Version.zero(), null, false)
  }

  fun withoutQualifiers() =
    VersionRange(minInclusive, minVersion.withoutQualifier(), maxVersion?.withoutQualifier(), maxInclusive)

  override fun toString(): String {
    return "${if(minInclusive) "[" else "("}$minVersion,${maxVersion ?: ""}${if(maxInclusive) "]" else ")"}"
  }
}

sealed class VersionOrRange {
  companion object {
    internal fun parse(str: String, log: Log): VersionOrRange? {
      val parsedVersion = Version.parse(str)
      if(parsedVersion != null) {
        return parsedVersion
      }
      val parsedVersionRange = VersionRange.parse(str)
      if(parsedVersionRange != null) {
        return parsedVersionRange
      }
      log.warning("Failed to parse version or version range '$str', defaulting to no version (matches any version)")
      return null
    }
  }
}
