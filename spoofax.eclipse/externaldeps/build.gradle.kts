buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("biz.aQute.bnd:biz.aQute.bnd.gradle:4.1.0")
  }
}
version = "1.0.0-SNAPSHOT"

// Add dependencies to JVM (non-OSGi) libraries
plugins {
  `java-library`
}
dependencies {
  api("org.metaborg:log.slf4j:develop-SNAPSHOT")
  api("org.metaborg:pie.runtime:develop-SNAPSHOT")
  api("org.metaborg:spoofax.runtime:develop-SNAPSHOT")
  api("org.metaborg:spoofax.pie:develop-SNAPSHOT")
}

// Use bnd to create a single OSGi bundle that includes all dependencies.
apply(plugin = "biz.aQute.bnd.builder")
val exports = listOf(
  "mb.*",
  "org.slf4j.*;provider=mb;mandatory:=provider",
  "kotlin.*;-split-package:=first;provider=mb;mandatory:=provider"
)
val imports = listOf(
  "!org.slf4j.impl",
  "*"
)
val jar: Jar by tasks
jar.apply {
  manifest {
    attributes(Pair("Export-Package", exports.joinToString(", ")))
    attributes(Pair("Import-Package", imports.joinToString(", ")))
    attributes(Pair("Bundle-Version", version.replace("-SNAPSHOT", ".qualifier")))
  }
}

// Export the produced OSGi bundle JAR as an 'eclipse-plugin' artifact.
configurations.create("eclipse-plugin")
artifacts {
  add("eclipse-plugin", jar)
}
