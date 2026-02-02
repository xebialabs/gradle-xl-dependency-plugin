package com.xebialabs.gradle.dependency;

import com.xebialabs.gradle.dependency.domain.GroupArtifact;
import com.xebialabs.gradle.dependency.tasks.ExportConfTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.Map;

public class XLDependencyPlatformPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    String projectName = project.getName();
    project.getLogger().warn(project.getPath() + ": Applying dependency management platform");

    DependencyManagementPlatformPluginExtension platformExtension = 
      project.getExtensions().create("xlPlatform", DependencyManagementPlatformPluginExtension.class);

    project.getPluginManager().withPlugin("java-platform", appliedPlugin -> {
      DependencyManagementExtension dependencyManagementExtension = 
        project.getRootProject().getExtensions().getByType(DependencyManagementExtension.class);
      DependencyManagementContainer dependencyManagementContainer = dependencyManagementExtension.getContainer();

      project.afterEvaluate(evaluatedProject -> {
        dependencyManagementContainer.resolveIfNecessary();
        project.getDependencies().constraints(dependencyConstraintHandler -> {
          if (platformExtension.getRestrictDependenciesEnabled().get()) {
            rejectBlacklistDependencies(dependencyManagementContainer, dependencyConstraintHandler);
          }
          if (platformExtension.getPinVersions().get()) {
            pinManagedDependenciesVersions(dependencyManagementContainer, dependencyConstraintHandler, project, projectName);
          }
        });
        configureDependencyManagementJavaPlatform(project, dependencyManagementContainer);
      });
    });
  }

  private void configureDependencyManagementJavaPlatform(Project project, DependencyManagementContainer dependencyManagementContainer) {
    Configuration confFileConfiguration = project.getConfigurations().create("confFile");
    confFileConfiguration.setCanBeResolved(false);
    confFileConfiguration.setCanBeConsumed(true);
    confFileConfiguration.attributes(attributes -> {
      attributes.attribute(
        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
        project.getObjects().named(LibraryElements.class, "conf-file")
      );
    });

    TaskProvider<ExportConfTask> exportConfTask = project.getTasks()
      .register("exportDependencyManagementConf", ExportConfTask.class, project.getObjects(), project.getLayout());
    
    exportConfTask.configure(task -> {
      // outputs.upToDateWhen { false } // we have inputFiles just to detect if anything changed
      task.setGroup("publishing");
      task.setInputFiles(dependencyManagementContainer.getSuppliedConfigFiles());
      File outputFile = project.getLayout().getBuildDirectory()
        .file(project.getName() + ".conf").get().getAsFile();
      task.getOutputFile().set(outputFile);
      task.setDependencyManagementContainer(dependencyManagementContainer);
    });

    ConfigurablePublishArtifact exportedConfFileArtifact = (ConfigurablePublishArtifact) project.getArtifacts()
      .add(confFileConfiguration.getName(), exportConfTask.get().getOutputFile(), artifact -> {
        artifact.setExtension("conf");
      });

    var javaPlatform = project.getComponents().named("javaPlatform");
    
    project.getPluginManager().withPlugin("maven-publish", appliedPlugin -> {
      project.getExtensions().configure(PublishingExtension.class, publishing -> {
        publishing.getPublications().create("dependencyManagement", MavenPublication.class, publication -> {
          publication.setArtifactId(project.getName());
          publication.artifact(exportedConfFileArtifact);
          publication.from(javaPlatform.get());
          publication.pom(pom -> {
            pom.setPackaging("pom");
            pom.getDescription().set("BOM defined via dependency manager plugin");
          });
        });
      });
    });
  }

  private void pinManagedDependenciesVersions(
      DependencyManagementContainer dependencyManagementContainer,
      DependencyConstraintHandler dependencyConstraintHandler,
      Project project,
      String projectName) {
    
    for (Map.Entry<String, String> entry : dependencyManagementContainer.getManagedVersions().entrySet()) {
      String artifactModule = entry.getKey();
      String artifactVersion = entry.getValue();
      
      if (artifactVersion != null) {
        artifactVersion = artifactVersion.trim();
      }
      
      if (artifactVersion != null && !artifactVersion.isEmpty()) {
        if (artifactVersion.startsWith("${")) {
          project.getLogger().info("Will not add " + artifactModule + " to " + projectName + 
            " as " + artifactVersion + " is not resolved version");
        } else {
          final String version = artifactVersion; // effectively final for lambda
          dependencyConstraintHandler.add("api", artifactModule, constraint -> {
            constraint.version(versionConstraint -> {
              versionConstraint.strictly(version);
              // NOTE: preferred versions will not be included into generated pom.xml
              // versionConstraint.prefer(version);
            });
            constraint.because("version was set by dependency manager");
          });
          project.getLogger().info("Added " + artifactModule + ":" + artifactVersion + " to " + projectName);
        }
      } else {
        project.getLogger().info("Unable to add " + artifactModule + " to " + projectName);
      }
    }
  }

  private void rejectBlacklistDependencies(
      DependencyManagementContainer dependencyManagementContainer,
      DependencyConstraintHandler dependencyConstraintHandler) {
    
    for (GroupArtifact artifact : dependencyManagementContainer.getBlackList()) {
      dependencyConstraintHandler.add("api", artifact.getGroup() + ":" + artifact.getArtifact(), constraint -> {
        constraint.version(versionConstraint -> {
          versionConstraint.rejectAll();
        });
        constraint.because("rejected use by dependency manager");
      });
    }
  }
}
