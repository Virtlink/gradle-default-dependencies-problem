package mb.releng.eclipse.mavenize

import mb.releng.eclipse.util.StreamLog
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MavenizeTest {
  @Test
  fun mavenize() {
    val log = StreamLog()
    /**
     * Choose url from:
     * - Drops    : http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
     * - Releases : http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
     */
    val url = "http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R-win32-x86_64.zip"
    val groupId = "eclipse-photon"
    val mavenizeDir = Paths.get(System.getProperty("user.home"), ".mavenize")
    val mavenizer = Mavenizer(mavenizeDir, groupId, log)
    //mavenizer.mavenize(url, true, true)
  }
}
