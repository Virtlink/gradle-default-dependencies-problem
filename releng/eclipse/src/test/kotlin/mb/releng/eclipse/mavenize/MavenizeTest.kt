package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.TempDir
import mb.releng.eclipse.util.unpack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MavenizeTest {
  @Test
  fun mavenize() {
    val mavenizeDirectory = Paths.get("""C:\Users\Gohla\.mavenize\""")
    val mirror = "http://ftp.fau.de"
    // Choose path and filename from:
    // * Drops    - http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/ TODO: has no sha512 file!
    // * Releases - http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
    val path = "eclipse/technology/epp/downloads/release/photon/R/"
    val filename = "eclipse-committers-photon-R-win32-x86_64.zip"
    val eclipseArchiveFile = retrieveEclipseArchive(mirror, path, filename, mavenizeDirectory.resolve("eclipse_archive_cache"))
    TempDir("eclipse-archive-unpack").use { tempDir ->
      Files.newInputStream(eclipseArchiveFile).buffered().use {
        unpack(it, filename, tempDir.path)
      }
      val pluginsDir = tempDir.path.resolve("eclipse/plugins")
      EclipseBundleInstaller(mavenizeDirectory.resolve("repo"), "eclipse-photon").use {
        it.installAllFromDirectory(pluginsDir)
      }
    }
  }
}
