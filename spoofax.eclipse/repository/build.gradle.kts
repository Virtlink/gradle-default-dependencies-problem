
buildscript {
  repositories {
    flatDir { dirs("../../releng/eclipse.gradle/build/libs") }
  }
  dependencies {
    classpath("org.metaborg", "releng.eclipse.gradle", "develop-SNAPSHOT")
  }
}
apply {
  plugin("org.metaborg.eclipse-repository")
}