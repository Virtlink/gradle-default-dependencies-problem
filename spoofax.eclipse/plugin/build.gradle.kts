buildscript {
  dependencies {
    classpath("org.metaborg", "releng.eclipse.gradle", "develop-SNAPSHOT")
  }
}
apply {
  plugin("org.metaborg.eclipse-plugin")
}
//plugins {
//  id("mb.releng.eclipse.gradle.eclipse-plugin") version "develop-SNAPSHOT"
//}
