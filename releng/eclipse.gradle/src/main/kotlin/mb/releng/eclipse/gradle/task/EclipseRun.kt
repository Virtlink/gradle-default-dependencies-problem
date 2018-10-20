package mb.releng.eclipse.gradle.task

import mb.releng.eclipse.mavenize.MavenizedEclipseInstallation
import org.gradle.api.tasks.JavaExec

open class EclipseRun : JavaExec() {
  fun configure(prepareEclipseRunConfig: PrepareEclipseRunConfig, installation: MavenizedEclipseInstallation) {
    dependsOn(prepareEclipseRunConfig)
    val configFile = prepareEclipseRunConfig.eclipseRunConfigFile
    inputs.file(configFile)
    workingDir = configFile.parent.toFile()
    main = "-Dosgi.configuration.cascaded=true"
    args(
      "-Dosgi.sharedConfiguration.area=.",
      "-Dosgi.sharedConfiguration.area.readOnly=true",
      "-Dosgi.configuration.area=configuration",
      "-jar", installation.equinoxLauncherPath(),
      "-data", "workspace",
      "-consoleLog"
    )
  }
}