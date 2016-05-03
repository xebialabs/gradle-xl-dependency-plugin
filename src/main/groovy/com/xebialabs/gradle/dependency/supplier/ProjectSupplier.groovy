package com.xebialabs.gradle.dependency.supplier

import com.xebialabs.gradle.dependency.DependencyManagementContainer
import org.gradle.api.Project

class ProjectSupplier implements DependencyManagementSupplier {

  public static final String VERSION_KEY_PREFIX = "dependencyManagement.versions."
  private Project project

  ProjectSupplier(Project project) {
    this.project = project
  }

  @Override
  def collectDependencies(DependencyManagementContainer container) {
    // Do nothing
  }

  @Override
  def collectVersions(DependencyManagementContainer container) {
    def versionOverrides = project.properties.findAll { k, v -> k.startsWith(VERSION_KEY_PREFIX) }
    if (versionOverrides) {
      versionOverrides.each { k, v ->
        container.registerVersionKey(k[VERSION_KEY_PREFIX.length()..-1], v as String)
      }
    }
  }

  @Override
  def collectExclusions(DependencyManagementContainer container) {
    // Do nothing
  }

  @Override
  def collectRewrites(DependencyManagementContainer container) {
    // Do nothing
  }
}
