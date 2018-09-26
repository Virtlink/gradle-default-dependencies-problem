package mb.releng.eclipse.mavenize

import java.io.PrintWriter

/**
 * Metadata for a Maven artifact.
 */
data class MavenMetadata(
  val groupId: String,
  val artifactId: String,
  val version: String,
  val dependencies: Collection<MavenDependency>
) {
  fun toPomXml(writer: PrintWriter) {
    writer.println("""<?xml version="1.0" encoding="UTF-8"?>""")
    writer.println("""<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">""")
    writer.println("""  <modelVersion>4.0.0</modelVersion>""")
    writer.println("""  <groupId>$groupId</groupId>""")
    writer.println("""  <artifactId>$artifactId</artifactId>""")
    writer.println("""  <version>$version</version>""")
    if(!dependencies.isEmpty()) {
      writer.println()
      writer.println("""  <dependencies>""")
      for(dependency in dependencies) {
        dependency.toPomXml(writer)
      }
      writer.println("""  </dependencies>""")
    }
    writer.println("""</project>""")
  }
}

/**
 * A Maven dependency.
 */
data class MavenDependency(
  val groupId: String,
  val artifactId: String,
  val version: String,
  val optional: Boolean
) {
  fun toPomXml(writer: PrintWriter) {
    writer.println("""    <dependency>""")
    writer.println("""      <groupId>$groupId</groupId>""")
    writer.println("""      <artifactId>$artifactId</artifactId>""")
    writer.println("""      <version>$version</version>""")
    if(optional) {
      writer.println("""      <optional>true</optional>""")
    }
    writer.println("""    </dependency>""")
  }
}
