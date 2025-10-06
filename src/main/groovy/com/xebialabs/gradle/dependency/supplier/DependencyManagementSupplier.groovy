package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.*
import com.xebialabs.gradle.dependency.DependencyManagementContainer
import groovy.util.ConfigObject

abstract class DependencyManagementSupplier {

  abstract def collectDependencies(DependencyManagementContainer container)

  abstract def collectVersions(DependencyManagementContainer container)

  abstract def collectExclusions(DependencyManagementContainer container)

  abstract def collectRewrites(DependencyManagementContainer container)

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

  def collectRewrites(Config config, DependencyManagementContainer container) {
    if (config.hasPath("dependencyManagement.rewrites")) {
      parseRewrites(config.getConfig("dependencyManagement.rewrites"), container)
    }
  }

  private def parseVersions(config, dependencyManagementContainer) {
    config.entrySet().each { e ->
      String key = e.key.startsWith('"') ? e.key.substring(1, e.key.length() - 1) : e.key
      def value = e.value.respondsTo("unwrapped") ? e.value.unwrapped() : e.value
      dependencyManagementContainer.registerVersionKey(key, value as String)
    }
  }


  private def parseDependencies(ConfigList list, DependencyManagementContainer container) {
    list.each { ConfigValue v ->
      if (v.valueType() == ConfigValueType.STRING) {
        def gav = (v.unwrapped() as String).split('[:@]')
        container.addManagedVersion(gav[0], gav[1], gav[2])
      } else if (v.valueType() == ConfigValueType.OBJECT) {
        ConfigObject o = v as ConfigObject
        String group = o.get("group").unwrapped()
        String version = o.get("version").unwrapped()
        def emptyExcludes = ConfigValueFactory.fromIterable([])
        if (o.containsKey("artifacts")) {
          def excludes = (o.get("excludes", emptyExcludes) as ConfigList).collect { ConfigValue entry ->
            return entry.unwrapped() as String
          }
          (o.get("artifacts") as ConfigList).each { ConfigValue entry ->
            String artifact = entry.unwrapped() as String
            container.addManagedVersion(group, artifact, version, excludes)
          }
        } else {
          String artifact = o.get("artifact").unwrapped()
          def excludes = (o.get("excludes", emptyExcludes) as ConfigList).collect { ConfigValue entry ->
            return entry.unwrapped() as String
          }
          container.addManagedVersion(group, artifact, version, excludes)
        }
      }
    }
  }

  private def parseBlackList(ConfigList list, DependencyManagementContainer container) {
    list.each { ConfigValue cv ->
      def ga = (cv.unwrapped() as String).split(":")
      if (ga.length == 1) {
        container.blackList(ga[0], null)
      } else {
        container.blackList(ga[0], ga[1])
      }
    }
  }

  private def parseRewrites(config, container) {
    config.entrySet().each { e ->
      String key = e.key.startsWith('"') ? e.key.substring(1, e.key.length() - 1) : e.key
      def ga = key.split(":")
      def toGa = (e.value.unwrapped() as String).split(":")
      container.rewrite(ga[0], ga[1], toGa[0], toGa[1])
    }
  }

}
