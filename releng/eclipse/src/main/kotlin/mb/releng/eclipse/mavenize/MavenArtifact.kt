package mb.releng.eclipse.mavenize

data class MavenArtifact(
  val groupId: String,
  val artifactId: String,
  val version: String,
  val packaging: String,
  val dependencies: List<BundleDependency>
)

data class BundleDependency(
  val groupId: String,
  val artifactId: String,
  val version: String
)