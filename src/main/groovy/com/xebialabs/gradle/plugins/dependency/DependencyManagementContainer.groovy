package com.xebialabs.gradle.plugins.dependency

import com.xebialabs.gradle.plugins.dependency.supplier.DependencyManagementSupplier
import groovy.text.SimpleTemplateEngine
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtraPropertiesExtension

class DependencyManagementContainer {
    private static final Logger logger = Logging.getLogger(DependencyManagementContainer.class)

    List<DependencyManagementSupplier> suppliers = []
    SimpleTemplateEngine engine = new SimpleTemplateEngine()
    List<Project> projects = []

    Map versions = [:].withDefault { "" }
    Map managedVersions = [:]

    DependencyManagementContainer(Project project) {
        projects.addAll(project.allprojects)

    }

    def resolveIfNecessary() {
//        // First collect all versions
//        suppliers.each {
//            it.collectVersions(this)
//        }
//        // Then all keys
//        suppliers.each {
//            it.collectDependencies(this)
//        }
//        suppliers.clear()
    }

    def addSupplier(DependencyManagementSupplier supplier) {
//        suppliers.add(supplier)
        supplier.collectVersions(this)
        supplier.collectDependencies(this)
    }

    def registerVersionKey(String key, String version) {
        if (!versions[key]) {
            versions.put(resolve(key), version)
            // Also register the version key on each project, useful with for example $scalaVersion
            projects.each {
                it.extensions.getByType(ExtraPropertiesExtension).set(key, version)
            }
        }
    }

    def addManagedVersion(String group, String artifact, String version) {
        def ga = resolve("$group:$artifact")
        managedVersions[ga] = resolve(version)
    }

    def getManagedVersion(String group, String artifact) {
        String ga = "$group:$artifact"
        logger.debug("Trying to resolve version for $ga")
        if (managedVersions[ga]) {
            return managedVersions[ga]
        }

        return null
    }

    def resolve(String s) {
        return engine.createTemplate(s).make(versions).toString()
    }
}
