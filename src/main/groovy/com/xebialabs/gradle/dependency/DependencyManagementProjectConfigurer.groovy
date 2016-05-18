package com.xebialabs.gradle.dependency

import com.xebialabs.gradle.dependency.domain.GroupArtifact
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.plugins.MavenPlugin

class DependencyManagementProjectConfigurer {

  static def configureProject(Project project, DependencyManagementContainer container) {
    // Contract for all is that it executes the closure for all currently assigned objects, and any objects added later.
    project.getConfigurations().all { Configuration config ->
      if (config.name != 'zinc') { // The Scala compiler 'zinc' configuration should not be managed by us
        config.resolutionStrategy { ResolutionStrategy rs ->
          rs.eachDependency(rewrite(project, container))
          rs.eachDependency(forceVersion(project, container))
        }
        configureExcludes(project, config, container)
      }
    }
  }

  static def configureExcludes(Project project, Configuration config, DependencyManagementContainer container) {
    container.blackList.forEach { ga ->
      project.logger.info("Excluding ${ga.toMap()} from configuration ${config.getName()}")
      config.exclude ga.toMap()
    }
  }

  private static Action<? super DependencyResolveDetails> rewrite(Project project, DependencyManagementContainer container) {
    return new Action<DependencyResolveDetails>() {
      @Override
      void execute(DependencyResolveDetails details) {
        container.resolveIfNecessary()
        def rewrites = container.rewrites

        def fromGa = new GroupArtifact(details.requested.group, details.requested.name)
        GroupArtifact groupArtifact = rewrites[fromGa]
        if (groupArtifact) {
          project.logger.info("Rewriting $fromGa -> $groupArtifact")
          details.useTarget(groupArtifact.toMap(details.requested))
        }
      }
    }
  }

  private static Action<DependencyResolveDetails> forceVersion(Project project, DependencyManagementContainer container) {
    return new Action<DependencyResolveDetails>() {
      @Override
      void execute(DependencyResolveDetails details) {
        container.resolveIfNecessary()
        def version = container.getManagedVersion(details.requested.group, details.requested.name)
        if (version) {
          project.logger.info("Resolved version $version for ${details.requested.group}:${details.requested.name}")
          details.useVersion(version)
        } else {
          project.logger.info("No managed version for ${details.requested.group}:${details.requested.name} --> using version ${details.requested.version}")
        }
      }
    }
  }
}
