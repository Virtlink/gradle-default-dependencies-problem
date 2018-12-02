tasks {
  register("buildAll") {
    dependsOn(gradle.includedBuilds.map { it.task(":buildAll") })
  }
  register("cleanAll") {
    dependsOn(gradle.includedBuilds.map { it.task(":cleanAll") })
  }
//  register("runSpoofaxCli") {
//    dependsOn(gradle.includedBuild("spoofax").task(":spoofax.cli:run"))
//  }
//  register("runSpoofaxEclipse") {
//    dependsOn(gradle.includedBuild("spoofax.eclipse").task(":spoofax.eclipse.repository:run"))
//  }
//  register("spoofaxEclipseRepositoryDependencies") {
//    dependsOn(gradle.includedBuild("spoofax.eclipse").task(":spoofax.eclipse.repository:dependencies"))
//  }
}
