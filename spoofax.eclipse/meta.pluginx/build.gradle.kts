buildscript {
  repositories {
    // HACK: add our plugin's JAR and its dependencies as a repository, to make it available in IntelliJ, which
    // currently does not handle plugins in composite builds.
    flatDir { dirs("../../releng/eclipse.gradle/build/libs") }
//    flatDir { dirs("../../releng/eclipse/build/libs") }
    // Following repositories needed to resolve dependencies of our plugin.
    mavenCentral()
    jcenter()
  }
  dependencies {
    // HACK: add our plugin and its dependencies via classpath, instead of using a declarative plugin block.
    classpath("org.metaborg", "releng.eclipse.gradle", "develop-SNAPSHOT")
//    classpath("org.metaborg", "releng.eclipse", "develop-SNAPSHOT")
//    classpath("org.apache.maven.resolver:maven-resolver-api:1.1.1")
//    classpath("org.apache.maven.resolver:maven-resolver-impl:1.1.1")
//    classpath("org.apache.maven.resolver:maven-resolver-connector-basic:1.1.1")
//    classpath("org.apache.maven.resolver:maven-resolver-transport-file:1.1.1")
//    classpath("org.apache.maven:maven-resolver-provider:3.5.4")
//    classpath("org.apache.commons:commons-compress:1.18")
  }
}
apply {
  // HACK: apply our plugin, instead of using a declarative plugin block.
  plugin("org.metaborg.eclipse-plugin")
}

// HACK: add externaldeps as a Java API dependency, because IntelliJ does not recognize this from our plugin.
plugins {
  `java-library`
}
dependencies {
//  api(project(":spoofax.eclipse.externaldeps"))
}
