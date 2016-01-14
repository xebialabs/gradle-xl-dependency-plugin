package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.gradle.api.Project

class DependencySupplier extends ConfigSupplier {

    private String dependency
    private Config config
    private Project project

    DependencySupplier(Project project, String dependency) {
        this.project = project
        this.dependency = dependency
    }

    Config getConfig() {
        if (!config) {
            def dependency = project.dependencies.create(dependency + "@conf")
            def resolve = project.configurations.detachedConfiguration(dependency).resolve()
            assert resolve.size() == 1 : "Dependency ${dependency} resulted in more than 1 file: $resolve"
            config = ConfigFactory.parseFile(resolve.find())
        }
        return config
    }
}
