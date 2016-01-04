package com.xebialabs.gradle.plugins.dependency

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import groovy.text.SimpleTemplateEngine
import io.spring.gradle.dependencymanagement.DependencyManagementContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtraPropertiesExtension

import static com.xebialabs.gradle.plugins.dependency.DependencyUtils.adHocDependency

class XLDependencyManagement {
    Project project
    SimpleTemplateEngine engine

    def XLDependencyManagement(Project project) {
        this.project = project
        this.engine = new SimpleTemplateEngine()
    }

    def dependenciesArtifact(DependencyManagementContainer container, Configuration configuration, String artifact) {
        adHocDependency(this.project, artifact + "@conf").resolve().forEach {
            dependenciesFile(container, configuration, it)
        }
    }

    def dependenciesFile(DependencyManagementContainer container, Configuration configuration, File file) {
        def config = ConfigFactory.parseFile(file)
        if (config.hasPath("versions")) {
            new VersionManagement(this.project).parseVersions(config.getConfig("versions"))
        }
        if (config.hasPath("dependencies")) {
            parseDependencies(config.getList("dependencies"), container, configuration)
        }
    }

    def parseDependencies(ConfigList list, DependencyManagementContainer container, Configuration configuration) {
        def bindings = project.rootProject.extensions.findByType(ExtraPropertiesExtension).properties
        list.forEach({ ConfigValue v ->
            if (v.valueType() == ConfigValueType.STRING) {
                String d = resolve(v, bindings)
                def gav = d.toString().split(':')
                container.addExplicitManagedVersion(null, gav[0], gav[1], gav[2], [])
            }
            else if (v.valueType() == ConfigValueType.OBJECT) {
                ConfigObject o = v as ConfigObject
                String group = o.get("group").unwrapped()
                String version = resolve(o.get("version"), bindings)
                (o.get("entries") as ConfigList).forEach({ ConfigValue entry ->
                    container.addExplicitManagedVersion(null, group, entry.unwrapped().toString(), version, [])
                })

            }
        })
    }

    private String resolve(ConfigValue v, Map bindings) {
        engine.createTemplate(v.unwrapped().toString()).make(bindings).toString()
    }


}
