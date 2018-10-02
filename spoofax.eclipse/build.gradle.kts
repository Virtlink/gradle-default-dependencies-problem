subprojects {
  group = "org.metaborg"
  version = "develop-SNAPSHOT"
  repositories {
    mavenCentral()
    jcenter()
  }
}
tasks {
  register("buildAll") {
    dependsOn(project(":spoofax.eclipse.plugin").tasks["build"])
  }
  register("cleanAll") {
    dependsOn(project(":spoofax.eclipse.plugin").tasks["clean"])
  }
  register("testEclipsePluginBuild") {
    //dependsOn(project(":spoofax.eclipse.plugin").tasks["mavenizeTargetPlatform"])
  }
}
