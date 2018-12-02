subprojects {
  repositories {
    mavenCentral()
  }
}
tasks {
  register("buildAll") {
    dependsOn(subprojects.map { it.tasks["build"] })
  }
}
