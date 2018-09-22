rootProject.name = "log"

include("log.api")
include("log.slf4j")

project(":log.api").projectDir = file("api")
project(":log.slf4j").projectDir = file("slf4j")
