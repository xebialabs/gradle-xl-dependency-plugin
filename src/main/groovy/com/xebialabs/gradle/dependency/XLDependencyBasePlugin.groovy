package com.xebialabs.gradle.dependency

import org.gradle.api.Plugin
import org.gradle.api.Project

class XLDependencyBasePlugin implements Plugin<Project> {
  Project project

  public void apply(Project project) {
    if (project.rootProject != project) {
      throw new IllegalArgumentException("Can apply 'xebialabs.dependency.base' only on the rootProject. Tried to apply on ${project.name}.")
    }
    this.project = project
    DependencyManagementContainer container = new DependencyManagementContainer(project)
    project.getExtensions().create("dependencyManagement", DependencyManagementExtension.class, project, container);

    project.allprojects.each { Project p ->
      DependencyManagementProjectConfigurer.configureProject(p, container)
    }
  }
}
