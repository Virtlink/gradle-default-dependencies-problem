buildscript {
  repositories {
    flatDir { dirs("../../my-gradle-plugin/build/libs") }
  }
  dependencies {
    classpath("org.plugin", "my-gradle-plugin", "develop-SNAPSHOT")
  }
}
apply {
  plugin("my.plugin.my-plugin-repository")
}