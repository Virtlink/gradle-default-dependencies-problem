pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if(requested.id.id.startsWith("org.metaborg")) {
        useModule("org.metaborg:eclipse.gradle:${requested.version}")
      }
    }
  }
}

rootProject.name = "spoofax.eclipse"

include("spoofax.eclipse.feature")
include("spoofax.eclipse.repository")

project(":spoofax.eclipse.feature").projectDir = file("feature")
project(":spoofax.eclipse.repository").projectDir = file("repository")
