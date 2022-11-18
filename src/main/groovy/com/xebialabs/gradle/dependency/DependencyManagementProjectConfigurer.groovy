package com.xebialabs.gradle.dependency

import com.xebialabs.gradle.dependency.domain.GroupArtifact
import com.xebialabs.gradle.dependency.rules.DependencyManagementExclusionRules
import com.xebialabs.gradle.dependency.rules.DependencyManagementRewriteRules
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ResolutionStrategy

class DependencyManagementProjectConfigurer {

  static def configureProject(Project project, DependencyManagementContainer container) {
    // Contract for all is that it executes the closure for all currently assigned objects, and any objects added later.
    project.getConfigurations().all { Configuration config ->
      if (!container.useJavaPlatform) {
        if (config.name != 'zinc') { // The Scala compiler 'zinc' configuration should not be managed by us
          config.resolutionStrategy { ResolutionStrategy rs ->
            rs.eachDependency(manageDependency(project, container))
          }
          configureExcludes(project, config, container)
        }
      }
    }
  }

  private static def configureExcludes(Project project, Configuration config, DependencyManagementContainer container) {
    container.blackList.each { ga ->
      if (!container.rewrites[ga]) {
        // exclude only dependencies that do NOT have a rewrite
        project.logger.debug("Excluding ${ga.toMap()} from configuration ${config.getName()}")
        config.exclude ga.toMap()
      }
    }
  }

  private static Action<? super DependencyResolveDetails> manageDependency(Project project, DependencyManagementContainer container) {
    return new Action<DependencyResolveDetails>() {
      @Override
      void execute(DependencyResolveDetails details) {
        container.resolveIfNecessary()
        rewrite(details)
        enforceVersion(details)
      }

      private void rewrite(DependencyResolveDetails details) {
        def rewrites = container.rewrites

        def fromGa = new GroupArtifact(details.requested.group, details.requested.name)
        GroupArtifact groupArtifact = rewrites[fromGa]
        if (groupArtifact) {
          def requestedVersion = container.getManagedVersion(details.requested.group, details.requested.name) ?: details.requested.version
          def rewriteVersion = container.getManagedVersion(groupArtifact.group, groupArtifact.artifact)
          if (rewriteVersion) {
            groupArtifact = groupArtifact.withVersion(rewriteVersion)
          } else {
            groupArtifact = groupArtifact.withVersion(requestedVersion)
          }
          project.logger.debug("Rewriting $fromGa -> $groupArtifact")
          details.useTarget(groupArtifact.toMap(details.requested))

        }
      }

      private void enforceVersion(DependencyResolveDetails details) {
        def version = container.getManagedVersion(details.requested.group, details.requested.name)
        if (version) {
          if (version.startsWith("\${")) {
            // version is not resolved
            project.logger.info("Unresolved version $version for ${details.requested.group}:${details.requested.name}. Will not use it as a version.")
          } else {
            project.logger.debug("Resolved version $version for ${details.requested.group}:${details.requested.name}")
            details.useVersion(version)
          }
        } else {
          project.logger.debug("No managed version for ${details.requested.group}:${details.requested.name} --> using version ${details.requested.version}")
        }
      }
    }
  }

  def static configureRewrites(DependencyManagementContainer container, Project project) {
    // always rewrite dependencies - even when using legacy plugin implementation
    project.getDependencies().modules(new DependencyManagementRewriteRules(container, project))
  }

  static def configureExcludeRules(DependencyManagementContainer container, Project project) {
    if (container.useJavaPlatform) {
      project.getDependencies().components(new DependencyManagementExclusionRules(container, project))
    }
  }
}
