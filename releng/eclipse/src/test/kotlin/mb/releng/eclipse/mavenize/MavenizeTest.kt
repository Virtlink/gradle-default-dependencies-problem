package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.StreamLog
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MavenizeTest {
  @Test
  fun mavenize() {
    val log = StreamLog()
    val mavenizeDir = Paths.get(System.getProperty("user.home"), ".mavenize")
    val eclipseArchiveRetriever = EclipseArchiveRetriever(
      // Choose file from:
      // * Drops    - http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
      // * Releases - http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
      "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip",
      mavenizeDir.resolve("eclipse_archive_cache")
    )
    val hasUnpacked = eclipseArchiveRetriever.getArchive(log)
    val eclipseBundlesPath = eclipseArchiveRetriever.unpackDir.resolve(Paths.get("eclipse", "plugins"))
    if(hasUnpacked) {
      EclipseBundleInstaller(mavenizeDir.resolve("repo"), "eclipse-photon").use { installer ->
        installer.installAllFromDirectory(eclipseBundlesPath, log)
      }
    } else {
      log.progress("Skipping installation of bundles, as directory $eclipseBundlesPath has not changed")
    }
  }
}
