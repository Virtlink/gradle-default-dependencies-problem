tasks {
  register("build") {
    dependsOn(gradle.includedBuilds.map { it.task(":build") })
  }
}
