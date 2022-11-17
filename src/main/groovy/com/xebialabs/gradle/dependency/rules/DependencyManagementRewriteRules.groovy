package com.xebialabs.gradle.dependency.rules

import com.xebialabs.gradle.dependency.DependencyManagementContainer
import com.xebialabs.gradle.dependency.domain.GroupArtifact
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler

class DependencyManagementRewriteRules implements Action<ComponentModuleMetadataHandler> {
  private DependencyManagementContainer container
  private Project project

  DependencyManagementRewriteRules(DependencyManagementContainer container, Project project) {
    this.container = container
    this.project = project
  }

  @Override
  void execute(final ComponentModuleMetadataHandler handler) {
    project.logger.debug("Configuring rewrites for project $project")
    def rewrites = container.rewrites
    rewrites.each {entry ->
      GroupArtifact fromGa = entry.key
      GroupArtifact toGa = entry.value
      project.logger.debug("Replacing $fromGa with $toGa")
      handler.module("${fromGa.group}:${fromGa.artifact}") {
        replacedBy("${toGa.group}:${toGa.artifact}", "replaced by dependency management plugin")
      }
    }
  }
}