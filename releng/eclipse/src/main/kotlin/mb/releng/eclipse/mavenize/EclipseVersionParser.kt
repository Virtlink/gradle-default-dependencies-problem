package mb.releng.eclipse.mavenize

import java.util.regex.Pattern


data class Version(val major: Int, val minor: Int?, val patch: Int?, val qualifier: String?) {
  companion object {
    private val pattern = Pattern.compile("""(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:\.(.+))?""")

    fun parse(str: String): Version? {
      val matcher = pattern.matcher(str)
      if(!matcher.matches()) return null
      val major = matcher.group(1)?.toInt() ?: return null
      val minor = matcher.group(2)?.toInt()
      val patch = matcher.group(3)?.toInt()
      val qualifier = matcher.group(4)
      return Version(major, minor, patch, qualifier)
    }

    fun zero(): Version {
      return Version(0, null, null, null)
    }
  }

  fun withoutQualifier(): Version {
    return Version(major, minor, patch, null)
  }

  override fun toString(): String {
    return "$major${if(minor != null) ".$minor" else ""}${if(patch != null) ".$patch" else ""}${if(qualifier != null) ".$qualifier" else ""}"
  }
}

data class VersionRange(val minInclusive: Boolean, val minVersion: Version, val maxVersion: Version?, val maxInclusive: Boolean) {
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

    fun anyVersionsRange(): VersionRange {
      return VersionRange(true, Version.zero(), null, false)
    }
  }

  override fun toString(): String {
    return "${if(minInclusive) "[" else "("}$minVersion,${maxVersion ?: ""}${if(maxInclusive) "]" else ")"}"
  }
}
