subprojects {
  repositories {
    mavenCentral()
  }
}
tasks {
  register("build") {
    dependsOn(subprojects.map { it.tasks["build"] })
  }
}
