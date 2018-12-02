rootProject.name = "releng"

include("releng.eclipse.gradle")

project(":releng.eclipse.gradle").projectDir = file("eclipse.gradle")
