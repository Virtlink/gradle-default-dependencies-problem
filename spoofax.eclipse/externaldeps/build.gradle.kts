buildscript {
  repositories {
    // HACK: add our plugin's JAR and its dependencies as a repository, to make it available in IntelliJ, which
    // currently does not handle plugins in composite builds.
    flatDir { dirs("../../releng/eclipse.gradle/build/libs") }
//    flatDir { dirs("../../releng/eclipse/build/libs") }
    // Following repositories needed to resolve dependencies of our plugin, and the bnd plugin.
    mavenCentral()
    jcenter()
  }
  dependencies {
    // HACK: add our plugin and its dependencies via classpath, instead of using a declarative plugin block.
    classpath("org.metaborg", "releng.eclipse.gradle", "develop-SNAPSHOT")
//    classpath("org.metaborg", "releng.eclipse", "develop-SNAPSHOT")
    classpath("org.apache.maven.resolver:maven-resolver-api:1.1.1")
    classpath("org.apache.maven.resolver:maven-resolver-impl:1.1.1")
    classpath("org.apache.maven.resolver:maven-resolver-connector-basic:1.1.1")
    classpath("org.apache.maven.resolver:maven-resolver-transport-file:1.1.1")
    classpath("org.apache.maven:maven-resolver-provider:3.5.4")
    classpath("org.apache.commons:commons-compress:1.18")
    // Add bnd plugin to classpath
    classpath("biz.aQute.bnd:biz.aQute.bnd.gradle:4.1.0")
  }
}
apply {
  // HACK: apply our plugin, instead of using a declarative plugin block.
  plugin("org.metaborg.eclipse-plugin")
  // Apply bnd plugin
  plugin("biz.aQute.bnd.builder")
}
// Add dependencies to JVM (non-OSGi) libraries
plugins {
  `java-library`
}
version = "1.0.0-SNAPSHOT"

dependencies {
//  api("org.metaborg:log.slf4j:develop-SNAPSHOT")
//  api("org.metaborg:pie.runtime:develop-SNAPSHOT")
//  api("org.metaborg:spoofax.runtime:develop-SNAPSHOT")
//  api("org.metaborg:spoofax.pie:develop-SNAPSHOT")
}

// Use bnd to create a single OSGi bundle that includes all dependencies.
val exports = listOf(
  "mb.*",
  "org.slf4j.*;provider=mb;mandatory:=provider",
  "kotlin.*;-split-package:=first;provider=mb;mandatory:=provider"
)
val jar: Jar by tasks
jar.apply {
  manifest {
    attributes(Pair("Export-Package", exports.joinToString(", ")))
    attributes(Pair("Import-Package", "")) // No imports needed
    attributes(Pair("Bundle-Version", version.replace("-SNAPSHOT", ".qualifier")))
  }
}

// Export the produced OSGi bundle JAR as an 'eclipsePlugin' artifact, for use in our Eclipse plugin.
val eclipsePlugin by configurations
artifacts {
  add(eclipsePlugin.name, jar)
}
