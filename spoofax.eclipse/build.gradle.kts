tasks {
  register("buildAll") {
    //dependsOn(subprojects.map { it.tasks["build"] })
  }
  register("cleanAll") {
    //dependsOn(subprojects.map { it.tasks["clean"] })
  }
}
