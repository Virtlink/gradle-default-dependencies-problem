rootProject.name = "spoofax"

include("spoofax.api")
include("spoofax.runtime")
include("spoofax.pie")
include("spoofax.cli")

project(":spoofax.api").projectDir = file("api")
project(":spoofax.runtime").projectDir = file("runtime")
project(":spoofax.pie").projectDir = file("pie")
project(":spoofax.cli").projectDir = file("cli")
