package com.xebialabs.gradle.dependency.rules

import com.xebialabs.gradle.dependency.DependencyManagementContainer
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler

class DependencyManagementExclusionRules implements Action<ComponentMetadataHandler> {
  private Project project
  private DependencyManagementContainer container

  DependencyManagementExclusionRules(DependencyManagementContainer container, Project project) {
    this.project = project
    this.container = container
  }

  @Override
  void execute(final ComponentMetadataHandler componentMetadataHandler) {
    def exclusionRule = new DependencyManagementExclusionRule(container)
    componentMetadataHandler.all(exclusionRule)
    // per component excludes:
    container.managedExcludes.each { entry ->
      def module = entry.key
      componentMetadataHandler.withModule(module, exclusionRule)
    }
  }
}