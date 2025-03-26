package com.xebialabs.gradle.dependency.rules

import com.xebialabs.gradle.dependency.DependencyManagementContainer
import com.xebialabs.gradle.dependency.domain.GroupArtifact
import org.gradle.api.Action
import org.gradle.api.artifacts.ComponentMetadataDetails

class DependencyManagementExclusionRule implements Action<ComponentMetadataDetails> {
  private HashSet<String> forbiddenDependencies = new HashSet<>()
  private DependencyManagementContainer container

  DependencyManagementExclusionRule(DependencyManagementContainer container) {
    this.container = container
    this.forbiddenDependencies.addAll(container.blackList.collect { ga -> "${ga.group}:${ga.artifact}".toString() })
  }

  @Override
  void execute(final ComponentMetadataDetails componentMetadataDetails) {
    String moduleName = "${componentMetadataDetails.id.group}:${componentMetadataDetails.id.name}".toString()
    Set<String> moduleExcludes = container.managedExcludes.get(moduleName, []).toSet()
    componentMetadataDetails.allVariants {
      withDependencies {
        removeIf { d ->
          String dependencyKey = "${d.group}:${d.name}".toString()
          def shouldBeExcluded = dependencyKey in forbiddenDependencies || dependencyKey in moduleExcludes
          def isNotRewritten = !container.rewrites[new GroupArtifact(d.group, d.name)]
          return shouldBeExcluded && isNotRewritten
        }
      }
    }
  }
}
