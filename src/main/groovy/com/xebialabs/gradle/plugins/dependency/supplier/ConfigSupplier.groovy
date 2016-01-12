package com.xebialabs.gradle.plugins.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import com.xebialabs.gradle.plugins.dependency.DependencyManagementContainer

abstract class ConfigSupplier implements DependencyManagementSupplier {

    abstract Config getConfig()

    @Override
    def collectDependencies(DependencyManagementContainer container) {
        collectDependencies(getConfig(), container)
    }

    @Override
    def collectVersions(DependencyManagementContainer container) {
        collectVersions(getConfig(), container)
    }

    @Override
    def collectExclusions(DependencyManagementContainer container) {
        collectExclusions(getConfig(), container)
    }

    def collectDependencies(Config config, DependencyManagementContainer container) {
        if (config.hasPath("dependencyManagement.dependencies")) {
            parseDependencies(config.getList("dependencyManagement.dependencies"), container)
        }
    }

    def collectVersions(Config config, DependencyManagementContainer container) {
        if (config.hasPath("dependencyManagement.versions")) {
            parseVersions(config.getConfig("dependencyManagement.versions"), container)
        }
    }

    def collectExclusions(Config config, DependencyManagementContainer container) {
        if (config.hasPath("dependencyManagement.blacklist")) {
            parseBlackList(config.getList("dependencyManagement.blacklist"), container)
        }
    }

    private def parseVersions(Config config, DependencyManagementContainer dependencyManagementContainer) {
        config.entrySet().each { e ->
            def key = e.key
            dependencyManagementContainer.registerVersionKey(key, e.value.unwrapped() as String)
        }
    }

    private def parseDependencies(ConfigList list, DependencyManagementContainer container) {
        list.forEach { ConfigValue v ->
            if (v.valueType() == ConfigValueType.STRING) {
                def gav = (v.unwrapped() as String).split(':')
                container.addManagedVersion(gav[0], gav[1], gav[2])
            }
            else if (v.valueType() == ConfigValueType.OBJECT) {
                ConfigObject o = v as ConfigObject
                String group = o.get("group").unwrapped()
                String version = o.get("version").unwrapped()
                (o.get("artifacts") as ConfigList).forEach { ConfigValue entry ->
                    container.addManagedVersion(group, entry.unwrapped() as String, version)
                }

            }
        }
    }

    private def parseBlackList(ConfigList list, DependencyManagementContainer container) {
        list.forEach { ConfigValue cv ->
            def ga = (cv.unwrapped() as String).split(":")
            if (ga.length == 1) {
                container.blackList(ga[0], null)
            } else {
                container.blackList(ga[0], ga[1])
            }
        }
    }
}
