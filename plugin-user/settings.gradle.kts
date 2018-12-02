pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if(requested.id.id.startsWith("org.metaborg")) {
        useModule("org.plugin:my-gradle-plugin:${requested.version}")
      }
    }
  }
}

rootProject.name = "plugin-user"

include("plugin-user.feature")
include("plugin-user.repository")

project(":plugin-user.feature").projectDir = file("feature")
project(":plugin-user.repository").projectDir = file("repository")
