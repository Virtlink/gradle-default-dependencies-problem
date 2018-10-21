pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if(requested.id.id.startsWith("org.metaborg")) {
        useModule("org.metaborg:releng.eclipse.gradle:${requested.version}")
      }
    }
  }
}

rootProject.name = "spoofax.eclipse"

include("spoofax.eclipse.plugin")
include("spoofax.eclipse.meta.plugin")
include("spoofax.eclipse.feature")
include("spoofax.eclipse.meta.feature")
include("spoofax.eclipse.repository")
include("spoofax.eclipse.externaldeps")

project(":spoofax.eclipse.plugin").projectDir = file("plugin")
project(":spoofax.eclipse.meta.plugin").projectDir = file("meta.plugin")
project(":spoofax.eclipse.feature").projectDir = file("feature")
project(":spoofax.eclipse.meta.feature").projectDir = file("meta.feature")
project(":spoofax.eclipse.repository").projectDir = file("repository")
project(":spoofax.eclipse.externaldeps").projectDir = file("externaldeps")
