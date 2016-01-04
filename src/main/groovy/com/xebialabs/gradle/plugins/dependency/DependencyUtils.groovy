package com.xebialabs.gradle.plugins.dependency

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class DependencyUtils {
    static Configuration adHocDependency(Project project, String notation) {
        def detachedConfiguration = project.configurations.detachedConfiguration(project.dependencies.create(notation))
        return detachedConfiguration
    }
}
