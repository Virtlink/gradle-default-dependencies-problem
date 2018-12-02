tasks {
  register("buildAll") {
    dependsOn(gradle.includedBuilds.map { it.task(":buildAll") })
  }
}
