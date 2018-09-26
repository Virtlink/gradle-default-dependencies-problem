package mb.releng.eclipse.mavenize

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.installation.InstallRequest
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.util.artifact.SubArtifact
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path


/**
 * Installs bundles as Maven artifacts into a local repository.
 */
class BundleInstaller(repositoryDir: Path, groupId: String) : AutoCloseable {
  private val converter = BundleConverter(groupId)
  private val tempDir = Files.createTempDirectory("bundle_installer")
  private val system: RepositorySystem
  private val session: RepositorySystemSession

  init {
    val locator = MavenRepositorySystemUtils.newServiceLocator()
    locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)

    // Uncomment for deployment over HTTP.
    //locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

    // Uncomment for routing error messages somewhere.
    //locator.setErrorHandler(object : DefaultServiceLocator.ErrorHandler() {
    //  fun serviceCreationFailed(type: Class<*>, impl: Class<*>, exception: Throwable) {
    //    LOGGER.error("Service creation failed for {} implementation {}: {}",
    //      type, impl, exception.message, exception)
    //  }
    //})

    system = locator.getService(RepositorySystem::class.java)
    session = MavenRepositorySystemUtils.newSession()
    val localRepo = LocalRepository(repositoryDir.toAbsolutePath().toString())
    session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)

    // Uncomment for routing logging messages regarding transfer progress and repositories somewhere.
    //session.transferListener = ConsoleTransferListener()
    //session.repositoryListener = ConsoleRepositoryListener()
  }

  override fun close() {
    // Delete contents of temporary directory and the directory itself.
    Files.walk(tempDir)
      .sorted(Comparator.reverseOrder())
      .forEach { Files.deleteIfExists(it) }
  }


  fun install(bundleFileOrDirectory: Path) {
    val bundleJar = when {
      !Files.exists(bundleFileOrDirectory) -> {
        throw IOException("Bundle file or directory $bundleFileOrDirectory does not exist")
      }
      Files.isDirectory(bundleFileOrDirectory) -> {
        TODO("Convert bundle from directory into a JAR file which can be installed into a Maven repository")
      }
      else -> {
        bundleFileOrDirectory
      }
    }
    val metadata = converter.convert(bundleJar)

    var jarArtifact: Artifact = DefaultArtifact(metadata.groupId, metadata.artifactId, "", "jar", metadata.version)
    jarArtifact = jarArtifact.setFile(bundleJar.toFile())

    val pomFile = Files.createTempFile(tempDir, "${metadata.artifactId}-${metadata.version}", ".pom")
    Files.newOutputStream(pomFile).buffered().use { outputStream ->
      PrintWriter(outputStream).use { writer ->
        metadata.toPomXml(writer)
        writer.flush()
      }
    }
    var pomArtifact: Artifact = SubArtifact(jarArtifact, "", "pom")
    pomArtifact = pomArtifact.setFile(pomFile.toFile())

    val installRequest = InstallRequest()
    installRequest.addArtifact(jarArtifact).addArtifact(pomArtifact)
    system.install(session, installRequest)
  }
}