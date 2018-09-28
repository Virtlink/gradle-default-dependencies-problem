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
    val eclipseArchiveFile = retrieveEclipseArchive(
      "http://ftp.fau.de",
      "eclipse/technology/epp/downloads/release/photon/R/",
      "eclipse-committers-photon-R-win32-x86_64.zip",
      mavenizeDirectory.resolve("eclipse_archive_cache")
    )
    TempDir("eclipse-archive-unpack").use { tempDir ->
      Files.newInputStream(eclipseArchiveFile).buffered().use {
        unpack(it, tempDir.path)
      }
      val pluginsDir = tempDir.path.resolve("eclipse/plugins")
      EclipseBundleInstaller(mavenizeDirectory.resolve("repo"), "eclipse-photon").use {
        it.installAllFromDirectory(pluginsDir)
      }
    }
  }
}
