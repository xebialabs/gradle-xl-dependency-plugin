package com.xebialabs.gradle.dependency;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class XLDependencyBasePlugin implements Plugin<Project> {
  
  private Project project;

  @Override
  public void apply(Project project) {
    if (project.getRootProject() != project) {
      throw new IllegalArgumentException("Can apply 'com.xebialabs.dependency.base' only on the rootProject. Tried to apply on " + project.getName() + ".");
    }
    this.project = project;
    DependencyManagementContainer container = new DependencyManagementContainer(project);
    project.getExtensions().create("dependencyManagement", DependencyManagementExtension.class, project, container);

    for (Project p : project.getAllprojects()) {
      DependencyManagementProjectConfigurer.configureProject(p, container);
    }
  }
}
