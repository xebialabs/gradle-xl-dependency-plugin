package com.xebialabs.gradle.plugins.dependency

import io.spring.gradle.dependencymanagement.DependencyManagementExtension
import io.spring.gradle.dependencymanagement.ImportsHandler
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class XLDependencyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.apply plugin: "xebialabs.dependency.base"

        project.dependencyManagement.imports {
            dependenciesFile "gradle/dependencies.conf"
        }
    }
}
