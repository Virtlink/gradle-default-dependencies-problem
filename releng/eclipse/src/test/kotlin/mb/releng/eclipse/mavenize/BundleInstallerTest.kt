package mb.releng.eclipse.mavenize

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BundleInstallerTest {
  @Test
  fun test() {
    val installer = BundleInstaller(Paths.get("""C:\Users\Gohla\.mavenize"""), "mavenized")
    installer.install(Paths.get("""C:\Users\Gohla\.wuff\unpacked\eclipse-SDK-4.8-win32-x86_64\plugins\org.eclipse.core.runtime_3.14.0.v20180417-0825.jar"""))
  }
}