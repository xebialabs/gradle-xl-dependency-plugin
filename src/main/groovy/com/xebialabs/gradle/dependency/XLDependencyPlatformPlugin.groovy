package com.xebialabs.gradle.dependency

import com.xebialabs.gradle.dependency.tasks.ExportConfTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider

class XLDependencyPlatformPlugin implements Plugin<Project> {

  void apply(Project project) {
    String projectName = project.getName()

    DependencyManagementPlatformPluginExtension platformExtension = project.extensions.create("xlPlatform", DependencyManagementPlatformPluginExtension)

    project.getPluginManager().withPlugin("java-platform") {

      def dependencyManagementExtension = project.getRootProject().extensions.getByType(DependencyManagementExtension)
      def dependencyManagementContainer = dependencyManagementExtension.container

      project.afterEvaluate {
        project.dependencies { DependencyHandler dependencyHandler ->
          dependencyHandler.with {
            constraints { DependencyConstraintHandler dependencyConstraintHandler ->
              if (platformExtension.restrictDependenciesEnabled.get()) {
                rejectBlacklistDependencies(dependencyManagementContainer, dependencyConstraintHandler)
              }
              if (platformExtension.pinVersions.get()) {
                pinManagedDependenciesVersions(dependencyManagementContainer, dependencyConstraintHandler, project, projectName)
              }
            }
          }
        }
      }

      configureDependencyManagementJavaPlatform(project, dependencyManagementContainer)
    }
  }

  def configureDependencyManagementJavaPlatform(Project project, DependencyManagementContainer dependencyManagementContainer) {
    Configuration confFileConfiguration = project.configurations.create("confFile")
    confFileConfiguration.with {
      canBeResolved = false
      canBeConsumed = true
      attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements, 'conf-file'))
      }
    }

    TaskProvider<ExportConfTask> exportConfTask = project.tasks.register("exportDependencyManagementConf", ExportConfTask, project.objects, project.layout)
    exportConfTask.configure {
      // outputs.upToDateWhen { false } // we have inputFiles just to detect if anything changed
      group = "publishing"
      inputFiles = dependencyManagementContainer.suppliedConfigFiles
      outputFile = project.file("${project.buildDir}/${project.name}.conf")
      setDependencyManagementContainer(dependencyManagementContainer)
    }
    def exportedConfFileArtifact = project.artifacts.add(confFileConfiguration.name, exportConfTask.get().outputFile) {
      ConfigurablePublishArtifact artifact ->
        artifact.with {
          extension = "conf"
        }
    }

    def javaPlatform = project.components.named("javaPlatform")
    project.pluginManager.withPlugin("maven-publish") {
      project.extensions.configure(PublishingExtension) {
        it.publications.create("dependencyManagement", MavenPublication) {
          artifactId = project.name
          artifact exportedConfFileArtifact
          from javaPlatform.get()
          pom {
            packaging = "pom"
            description.set('BOM defined via dependency manager plugin')
          }
        }
      }
    }
  }

  def pinManagedDependenciesVersions(DependencyManagementContainer dependencyManagementContainer,
                                     DependencyConstraintHandler dependencyConstraintHandler,
                                     Project project,
                                     String projectName) {
    dependencyManagementContainer.managedVersions.collect { entry ->
      String artifactModule = entry.key
      String artifactVersion = entry.value
      if (artifactVersion?.trim()) {
        dependencyConstraintHandler.add("api", artifactModule) {
          version {
            strictly(artifactVersion)
            // NOTE: preferred versions will not be included into generated pom.xml
            // prefer(artifactVersion)
          }
          because("version was set by dependency manager")
        }
        project.logger.info("Added $artifactModule:$artifactVersion to ${projectName}")
      } else {
        project.logger.info("Unable to add $artifactModule to ${projectName}")
      }
    }
  }

  def rejectBlacklistDependencies(DependencyManagementContainer dependencyManagementContainer, DependencyConstraintHandler dependencyConstraintHandler) {
    dependencyManagementContainer.blackList.collect { artifact ->
      dependencyConstraintHandler.add("api", "${artifact.group}:${artifact.artifact}") {
        version {
          rejectAll()
        }
        because("rejected use by dependency manager")
      }
    }
  }
}
