dependencies {
  compile("org.apache.maven.resolver:maven-resolver-api:1.1.1")
  compile("org.apache.maven.resolver:maven-resolver-impl:1.1.1")
  compile("org.apache.maven.resolver:maven-resolver-connector-basic:1.1.1")
  compile("org.apache.maven.resolver:maven-resolver-transport-file:1.1.1")
  compile("org.apache.maven:maven-resolver-provider:3.5.4")
  compile("org.apache.commons:commons-compress:1.18")

  testCompile("org.junit.jupiter:junit-jupiter-api:5.3.1")
  testRuntime("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}
tasks.withType<Test> {
  useJUnitPlatform {
    excludeTags.add("longRunning")
  }
}