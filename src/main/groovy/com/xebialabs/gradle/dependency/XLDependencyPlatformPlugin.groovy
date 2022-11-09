package com.xebialabs.gradle.dependency

import org.gradle.api.Plugin
import org.gradle.api.Project

class XLDependencyPlatformPlugin implements Plugin<Project> {
  private Project project

  void apply(Project project) {
    this.project = project
    String projectName = project.getName()
    def extension = project.getRootProject().extensions.getByType(DependencyManagementExtension)
    def dependencyManagementContainer = extension.container
    project.getPluginManager().withPlugin("java-platform") {
      // platform constraint
      project.dependencies {
        constraints {
          dependencyManagementContainer.blackList.collect { artifact ->
            api("${artifact.group}:${artifact.artifact}") {
              version {
                rejectAll()
              }
              because("restricted use by dependency manager")
            }
          }
          dependencyManagementContainer.managedVersions.collect { entry ->
            String artifactModule = entry.key
            String artifactVersion = entry.value
            if (artifactVersion?.trim()) {
              api("$artifactModule") {
                version {
                  strictly(artifactVersion)
                  // prefer(artifactVersion)
                }
                because("version was set by dependency manager")
              }
              project.logger.info("Added $artifactModule:$artifactVersion to ${projectName}")
            } else {
              project.logger.info("Unable to add $artifactModule to ${projectName} as a constraint")
            }
          }
        }
      }
    }
  }
}
