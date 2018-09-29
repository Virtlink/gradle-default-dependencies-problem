package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.Log
import mb.releng.eclipse.util.TempDir
import mb.releng.eclipse.util.packJar
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
import java.io.Closeable
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

/**
 * Installs Eclipse bundles as Maven artifacts into the local repository at [repositoryDir]. Since Eclipse bundles have
 * no consistent notion of group IDs, the given [groupId] is used.
 */
class EclipseBundleInstaller(repoDir: Path, groupId: String) : Closeable {
  val repoGroupIdDir: Path = repoDir.resolve(groupId)

  private val converter = EclipseBundleConverter(groupId)
  private val tempDir = TempDir("bundle_installer")
  private val system: RepositorySystem
  private val session: RepositorySystemSession

  init {
    if(!Files.exists(repoDir)) {
      Files.createDirectories(repoDir)
    } else if(!Files.isDirectory(repoDir)) {
      throw IOException("Repository at path $repoDir is not a directory")
    }

    val locator = MavenRepositorySystemUtils.newServiceLocator()
    locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)

    // Uncomment for deployment over HTTP.
    //locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

    // Uncomment for routing service creation error messages somewhere.
    //locator.setErrorHandler(object : DefaultServiceLocator.ErrorHandler() {
    //  fun serviceCreationFailed(type: Class<*>, impl: Class<*>, exception: Throwable) {
    //    LOGGER.error("Service creation failed for {} implementation {}: {}",
    //      type, impl, exception.message, exception)
    //  }
    //})

    system = locator.getService(RepositorySystem::class.java)
    session = MavenRepositorySystemUtils.newSession()
    val localRepo = LocalRepository(repoDir.toAbsolutePath().toString())
    session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)

    // Uncomment for routing logging messages regarding transfer progress and repositories somewhere.
    //session.transferListener = ConsoleTransferListener()
    //session.repositoryListener = ConsoleRepositoryListener()
  }

  override fun close() {
    tempDir.close()
  }


  /**
   * Installs bundle from JAR or directory [bundleJarOrDirectory] into the local repository.
   */
  fun installOneFromJarOrDirectory(bundleJarOrDirectory: Path, log: Log) {
    val installRequest = InstallRequest()
    addToInstallRequest(bundleJarOrDirectory, installRequest, log)
    system.install(session, installRequest)
  }

  /**
   * Installs all bundles found in [directory] into the local repository.
   */
  fun installAllFromDirectory(directory: Path, log: Log) {
    when {
      !Files.exists(directory) -> {
        throw IOException("Directory $directory does not exist")
      }
      !Files.isDirectory(directory) -> {
        throw IOException("Directory $directory is not a directory")
      }
    }
    log.progress("Requesting installation for all bundles in $directory")
    val installRequest = InstallRequest()
    Files.list(directory).forEach {
      addToInstallRequest(it, installRequest, log)
    }
    log.progress("Executing installation request")
    system.install(session, installRequest)
  }


  private fun addToInstallRequest(bundleFileOrDirectory: Path, installRequest: InstallRequest, log: Log) {
    val bundleJar = when {
      !Files.exists(bundleFileOrDirectory) -> {
        throw IOException("Bundle file or directory $bundleFileOrDirectory does not exist")
      }
      Files.isDirectory(bundleFileOrDirectory) -> {
        val jarFile = tempDir.createTempFile(bundleFileOrDirectory.fileName.toString(), ".jar")
        packJar(bundleFileOrDirectory, jarFile, log)
        jarFile
      }
      else -> {
        bundleFileOrDirectory
      }
    }
    val metadata = converter.convertBundleJarFile(bundleJar)

    var jarArtifact: Artifact = DefaultArtifact(metadata.groupId, metadata.artifactId, "", "jar", metadata.version)
    jarArtifact = jarArtifact.setFile(bundleJar.toFile())
    installRequest.addArtifact(jarArtifact)

    val pomFile = tempDir.createTempFile("${metadata.artifactId}-${metadata.version}", ".pom")
    Files.newOutputStream(pomFile).buffered().use { outputStream ->
      PrintWriter(outputStream).use { writer ->
        metadata.toPomXml(writer)
        writer.flush()
      }
    }
    var pomArtifact: Artifact = SubArtifact(jarArtifact, "", "pom")
    pomArtifact = pomArtifact.setFile(pomFile.toFile())
    installRequest.addArtifact(pomArtifact)

    log.debug("Requesting installation for ${bundleJar.fileName}")
  }
}