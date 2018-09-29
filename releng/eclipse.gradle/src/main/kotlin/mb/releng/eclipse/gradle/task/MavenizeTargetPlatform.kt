package mb.releng.eclipse.gradle.task

import mb.releng.eclipse.gradle.GradleLog
import mb.releng.eclipse.mavenize.EclipseArchiveRetriever
import mb.releng.eclipse.mavenize.EclipseBundleInstaller
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import javax.inject.Inject

open class MavenizeTargetPlatform @Inject constructor(
  @Input private val url: String,
  @Input private val groupId: String,
  @Input private val archiveBundlesPath: Path,
  @Input private val cacheDir: Path,
  @Input private val repoDir: Path
) : DefaultTask() {
  private val eclipseArchiveRetriever = EclipseArchiveRetriever(url, cacheDir)
  private val eclipseBundleInstaller = EclipseBundleInstaller(repoDir, groupId)

  @OutputFile
  val eclipseArchiveFile = eclipseArchiveRetriever.archiveFile
  @OutputDirectory
  val eclipseArchiveUnpackDir = eclipseArchiveRetriever.unpackDir
  @OutputDirectory
  val eclipsePluginsDir: Path = eclipseArchiveUnpackDir.resolve(archiveBundlesPath)
  @OutputDirectory
  val mavenizedPluginsDir = eclipseBundleInstaller.repoGroupIdDir

  @TaskAction
  fun run() {
    val log = GradleLog(logger)
    eclipseArchiveRetriever.getArchive(log)
    eclipseBundleInstaller.use {
      it.installAllFromDirectory(eclipsePluginsDir, log)
    }
  }
}