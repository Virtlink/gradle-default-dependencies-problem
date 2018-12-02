buildscript {
  repositories {
    flatDir { dirs("../../eclipse.gradle/build/libs") }
  }
  dependencies {
    classpath("org.metaborg", "eclipse.gradle", "develop-SNAPSHOT")
  }
}
apply {
  plugin("org.metaborg.eclipse-feature")
}
