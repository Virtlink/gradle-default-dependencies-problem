dependencies {
  api(project(":log.api"))
  compileOnly("com.google.code.findbugs:jsr305:3.0.2")
  api("org.slf4j:slf4j-api:1.7.25")
  api("com.google.inject:guice:4.2.0")
}
