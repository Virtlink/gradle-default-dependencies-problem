package mb.releng.eclipse.gradle.util

import mb.releng.eclipse.model.maven.DependencyCoordinates
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.project

fun DependencyCoordinates.toGradleDependency(project: Project, configuration: String?) =
  project.createDependency(groupId, id, version.toString(), configuration, classifier, extension)

fun DependencyCoordinates.toGradleDependencyNotation() =
  "$groupId:$id:$version${if(classifier != null) ":$classifier" else ""}${if(extension != null) "@$extension" else ""}"


private fun Project.createDependency(groupId: String, id: String, version: String, configuration: String?, classifier: String?, extension: String?): Dependency {
  val projectPath = ":$id"
  return if(project.findProject(projectPath) != null) {
    project.dependencies.project(projectPath, configuration)
  } else {
    project.dependencies.create(groupId, id, version, configuration, classifier, extension)
  }
}
