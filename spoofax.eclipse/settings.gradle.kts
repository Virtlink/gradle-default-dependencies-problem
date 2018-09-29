//pluginManagement {
//  resolutionStrategy {
//    eachPlugin {
//      if (requested.id.id.startsWith("mb.releng.eclipse.gradle")) {
//        useModule("org.metaborg:releng.eclipse.gradle:${requested.version}")
//      }
//    }
//  }
//}

rootProject.name = "spoofax.eclipse"

include("spoofax.eclipse.plugin")
include("spoofax.eclipse.feature")
include("spoofax.eclipse.updatesite")

project(":spoofax.eclipse.plugin").projectDir = file("plugin")
project(":spoofax.eclipse.feature").projectDir = file("feature")
project(":spoofax.eclipse.updatesite").projectDir = file("updatesite")
