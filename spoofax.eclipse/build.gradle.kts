subprojects {
  group = "org.metaborg"
  repositories {
    mavenCentral()
    jcenter()
  }
}
tasks {
  register("buildAll") {
    dependsOn(project(":spoofax.eclipse.plugin").tasks["build"])
    dependsOn(project(":spoofax.eclipse.feature").tasks["build"])
  }
  register("cleanAll") {
    dependsOn(project(":spoofax.eclipse.plugin").tasks["clean"])
    dependsOn(project(":spoofax.eclipse.feature").tasks["clean"])
  }
}
