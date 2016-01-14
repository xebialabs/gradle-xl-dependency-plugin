package com.xebialabs.gradle.dependency

import org.gradle.api.Plugin
import org.gradle.api.Project

class XLDependencyBasePlugin implements Plugin<Project> {
    Project project

    public void apply(Project project) {
        assert project.rootProject == project: "Please apply plugin 'xebialabs.dependency.base' on the rootProject only!"
        this.project = project
        DependencyManagementContainer container = new DependencyManagementContainer(project)
        project.getExtensions().create("dependencyManagement", DependencyManagementExtension.class, project, container);

        project.allprojects.each { Project p ->
            DependencyManagementProjectConfigurer.configureProject(p, container)
        }
    }

}
