buildscript {
  repositories {
    // HACK: add our plugin's JAR as a repository, to make it available in IntelliJ, which currently does not handle
    // plugins in composite builds.
    flatDir { dirs("../../releng/eclipse.gradle/build/libs") }
    // Following repositories needed to resolve dependencies of our plugin.
    mavenCentral()
    jcenter()
  }
  dependencies {
    // HACK: add our plugin via classpath, instead of using a declarative plugin block.
    classpath("org.metaborg", "releng.eclipse.gradle", "develop-SNAPSHOT")
  }
}
apply {
  // HACK: apply our plugin, instead of using a declarative plugin block.
  plugin("org.metaborg.eclipse-plugin")
}
