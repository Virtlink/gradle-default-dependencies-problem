package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.StreamLog
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MavenizeTest {
  @Test
  @Tag("longRunning")
  fun mavenize() {
    val log = StreamLog()
    val mavenizeDir = Paths.get(System.getProperty("user.home"), ".mavenize")
    val installationArchiveUrl = "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip"
    val installationPluginsDirRelative = Paths.get("eclipse", "plugins")
    val groupId = "eclipse-photon"

    val mavenizedEclipseInstallation1 = mavenizeEclipseInstallation(mavenizeDir, installationArchiveUrl, installationPluginsDirRelative, groupId, log, true, true)
    val mavenizedEclipseInstallation2 = mavenizeEclipseInstallation(mavenizeDir, installationArchiveUrl, installationPluginsDirRelative, groupId, log)
    Assertions.assertEquals(mavenizedEclipseInstallation1, mavenizedEclipseInstallation2)
  }
}
