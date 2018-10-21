buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("biz.aQute.bnd:biz.aQute.bnd.gradle:4.1.0")
  }
}
version = "1.0.0-SNAPSHOT"
plugins {
  `java-library`
}
apply(plugin = "biz.aQute.bnd.builder")
dependencies {
  api("org.metaborg:log.slf4j:develop-SNAPSHOT")
  api("org.metaborg:pie.runtime:develop-SNAPSHOT")
  api("org.metaborg:spoofax.runtime:develop-SNAPSHOT")
  api("org.metaborg:spoofax.pie:develop-SNAPSHOT")
}
val jar: Jar by tasks
jar.apply {
  manifest {
    attributes(Pair("Export-Package", "mb.*, org.slf4j.*;provider=mb;mandatory:=provider, !kotlin.js, kotlin.*;-split-package:=first;provider=mb;mandatory:=provider"))
    attributes(Pair("Import-Package", "mb.*, org.slf4j.*;provider=mb, !kotlin.js, kotlin.*;provider=mb"))
  }
}
configurations.create("eclipse-plugin")
artifacts {
  add("eclipse-plugin", jar)
}
