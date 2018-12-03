rootProject.name = "plugin-user"

include("plugin-user.feature")
include("plugin-user.repository")

project(":plugin-user.feature").projectDir = file("feature")
project(":plugin-user.repository").projectDir = file("repository")
