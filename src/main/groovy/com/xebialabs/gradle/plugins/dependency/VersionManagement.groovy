package com.xebialabs.gradle.plugins.dependency

import com.typesafe.config.Config
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension

class VersionManagement {

    Project project

    VersionManagement(Project project) {
        this.project = project
    }

    Object parseVersions(Config config) {
        def extension = project.ext as ExtraPropertiesExtension
        config.entrySet().each { e ->
            def key = e.key
            if (!extension.has(key)) {
                def value = e.value.unwrapped()
                extension[key] = value
            }
        }
    }
}
