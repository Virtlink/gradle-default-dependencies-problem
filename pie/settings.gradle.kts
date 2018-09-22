rootProject.name = "pie"

include("pie.api")
include("pie.runtime")

project(":pie.api").projectDir = file("api")
project(":pie.runtime").projectDir = file("runtime")
