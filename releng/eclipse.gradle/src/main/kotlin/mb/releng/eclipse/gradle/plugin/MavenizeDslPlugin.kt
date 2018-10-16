package mb.releng.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.property
import java.nio.file.Path
import java.nio.file.Paths

enum class EclipseOs(val archiveSuffix: String, val archiveExtension: String, val pluginsDir: Path) {
  Windows("win32", "zip", Paths.get("eclipse", "plugins")),
  Linux("linux", "tar.gz", Paths.get("eclipse", "plugins")),
  // TODO: OSX archive is in DMG format, temporarily treat it as Linux until we can unpack DMG archives.
  OSX("linux", "tar.gz", Paths.get("eclipse", "plugins"));
  //OSX("macosx", "dmg", Paths.get("TODO"));

  companion object {
    fun current(): EclipseOs {
      val os = System.getProperty("os.name")
      return when {
        os.substring(0, 5).equals("linux", true) -> Linux
        os.substring(0, 7).equals("windows", true) -> Windows
        os.equals("Mac OS X", true) -> OSX
        else -> error("Unsupported Eclipse OS '$os'")
      }
    }
  }
}

enum class EclipseArch(val archiveSuffix: String) {
  X86_32(""), X86_64("-x86_64");

  companion object {
    fun current(): EclipseArch {
      val arch = System.getProperty("os.arch")
      return when(arch) {
        "x86", "i386" -> X86_32
        "amd64" -> X86_64
        else -> error("Unsupported Eclipse architecture '$arch'")
      }
    }
  }
}

open class MavenizeExtension(objects: ObjectFactory) {
  val os: Property<EclipseOs> = objects.property()
  val arch: Property<EclipseArch> = objects.property()
  val mirrorUrl: Property<String> = objects.property()
  val prefixUrl: Property<String> = objects.property()
  val groupId: Property<String> = objects.property()
  val mavenizeDir: Property<Path> = objects.property()

  val url get() = "${mirrorUrl.get()}/${prefixUrl.get()}-${os.get().archiveSuffix}${arch.get().archiveSuffix}.${os.get().archiveExtension}"
}

class MavenizeDslPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = project.extensions.create<MavenizeExtension>("mavenize", project.objects)
    try {
      extension.os.set(EclipseOs.current())
    } catch(e: IllegalStateException) {
      project.logger.warn("Could not set default Eclipse OS for mavenizer", e)
    }
    try {
      extension.arch.set(EclipseArch.current())
    } catch(e: IllegalStateException) {
      project.logger.warn("Could not set default Eclipse architecture for mavenizer", e)
    }
    extension.mirrorUrl.set("http://ftp.fau.de")
    /**
     * Choose url prefix from:
     * - Drops    : http://ftp.fau.de/eclipse/eclipse/downloads/drops4/R-4.8-201806110500/
     * - Releases : http://ftp.fau.de/eclipse/technology/epp/downloads/release/photon/R/
     */
    extension.prefixUrl.set("eclipse/technology/epp/downloads/release/photon/R/eclipse-committers-photon-R")
    extension.groupId.set("eclipse-photon")
    extension.mavenizeDir.set(Paths.get(System.getProperty("user.home"), ".mavenize"))
  }
}
