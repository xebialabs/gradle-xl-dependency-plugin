package com.xebialabs.gradle.plugins.dependency

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails

class XLDependencyBasePlugin implements Plugin<Project> {

    public void apply(Project project) {
        assert project.rootProject == project: "Please apply plugin 'xebialabs.dependency.base' on the rootProject only!"

        DependencyManagementContainer container = new DependencyManagementContainer(project)
        project.getExtensions().create("dependencyManagement", DependencyManagementExtension.class, project, container);

        project.allprojects {
            configurations.all { configuration ->
                resolutionStrategy.eachDependency {
                    DependencyResolveDetails details ->
                        container.resolveIfNecessary()
                        def version = container.getManagedVersion(details.requested.group, details.requested.name)
                        if (version) {
                            project.logger.lifecycle("Resolved version $version for ${details.requested.group}:${details.requested.name}")
                            details.useVersion(version)
                        } else {
                            project.logger.lifecycle("Using version ${details.requested.version} for ${details.requested.group}:${details.requested.name}")
                        }
                }
            }
        }
    }
}
