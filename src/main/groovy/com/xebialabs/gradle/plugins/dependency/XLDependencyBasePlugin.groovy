package com.xebialabs.gradle.plugins.dependency

import io.spring.gradle.dependencymanagement.ImportsHandler
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class XLDependencyBasePlugin  implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project
        XLDependencyManagement dependencyManagement = new XLDependencyManagement(project)

        ImportsHandler.metaClass.dependenciesFile = { def file ->
            dependencyManagement.dependenciesFile(delegate.container, delegate.configuration, asFile(file))
        }

        ImportsHandler.metaClass.dependenciesArtifact = { def dependency ->
            dependencyManagement.dependenciesArtifact(delegate.container, delegate.configuration, dependency)
        }

        project.plugins.apply("io.spring.dependency-management")
    }

    def asFile(file) {
        if (file instanceof String) {
            return project.file(file)
        } else if (file instanceof File) {
            return file
        } else {
            throw new GradleException("$file is not a file or string")
        }
    }
}

