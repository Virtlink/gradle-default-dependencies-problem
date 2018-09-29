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
  }
  register("testEclipsePluginBuild") {
    dependsOn(project(":spoofax.eclipse.plugin").tasks["mavenizeTargetPlatform"])
  }
}
