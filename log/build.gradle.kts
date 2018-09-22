allprojects {
  group = "org.metaborg"
  version = "0.1.0-SNAPSHOT"
}
subprojects {
  repositories {
    mavenCentral()
  }
  apply(plugin = "java-library")
  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}
tasks {
  register("buildAll", GradleBuild::class) {
    setTasks(listOf("build"))
  }
  register("cleanAll", GradleBuild::class) {
    setTasks(listOf("clean"))
  }
}