rootProject.name = "releng"

include("releng.eclipse")
include("releng.eclipse.gradle")

project(":releng.eclipse").projectDir = file("eclipse")
project(":releng.eclipse.gradle").projectDir = file("eclipse.gradle")
