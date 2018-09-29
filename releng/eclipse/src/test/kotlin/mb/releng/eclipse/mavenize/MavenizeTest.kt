package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.StreamLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MavenizeTest {
  @Test
  fun mavenize() {
    val logger = StreamLogger()
    val mavenizeDirectory = Paths.get("""C:\Users\Gohla\.mavenize\""")
    // Choose path and filename from:
    // * Drops    - http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
    // * Releases - http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
    val prefix = "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/"
    val filenameWithoutExtension = "eclipse-committers-photon-R-win32-x86_64"
    val extension = "zip"
    val pluginPathInArchive = Paths.get("eclipse", "plugins")
    val cacheDirectory = mavenizeDirectory.resolve("eclipse_archive_cache")
    val (eclipseBundlesPath, hasUnpacked) = retrieveEclipsePluginBundlesFromArchive(prefix, filenameWithoutExtension, extension, pluginPathInArchive, cacheDirectory, logger)
    if(hasUnpacked) {
      EclipseBundleInstaller(mavenizeDirectory.resolve("repo"), "eclipse-photon").use {
        it.installAllFromDirectory(eclipseBundlesPath, logger)
      }
    } else {
      logger.progress("Skipping installation of bundles, as directory $eclipseBundlesPath has not changed")
    }
  }
}
