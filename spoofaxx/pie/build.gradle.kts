plugins {
  `java-library`
}
dependencies {
  api(project(":spoofax.api"))
  api("org.metaborg:pie.runtime:develop-SNAPSHOT")
}
