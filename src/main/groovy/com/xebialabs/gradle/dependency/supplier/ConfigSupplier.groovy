package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import com.xebialabs.gradle.dependency.DependencyManagementContainer
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

abstract class ConfigSupplier implements DependencyManagementSupplier {
  private static final Logger logger = Logging.getLogger(DependencyManagementContainer.class)

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

  @Override
  def collectRewrites(DependencyManagementContainer container) {
    collectRewrites(getConfig(), container)
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

  def collectRewrites(Config config, DependencyManagementContainer container) {
    if (config.hasPath("dependencyManagement.rewrites")) {
      parseRewrites(config.getConfig("dependencyManagement.rewrites"), container)
    }
  }

  private def parseVersions(Config config, DependencyManagementContainer dependencyManagementContainer) {
    config.entrySet().each { e ->
      String key = e.key.startsWith("\"") ? e.key[1..-2] : e.key
      dependencyManagementContainer.registerVersionKey(key, e.value.unwrapped() as String)
    }
  }


  private def parseDependencies(ConfigList list, DependencyManagementContainer container) {
    list.forEach { ConfigValue v ->
      if (v.valueType() == ConfigValueType.STRING) {
        def gav = (v.unwrapped() as String).split('[:@]')
        container.addManagedVersion(gav[0], gav[1], gav[2])
      } else if (v.valueType() == ConfigValueType.OBJECT) {
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

  private def parseRewrites(Config config, DependencyManagementContainer container) {
    config.entrySet().each { e ->
      String key = e.key.startsWith("\"") ? e.key[1..-2] : e.key
      def ga = key.split(":")
      def toGa = (e.value.unwrapped() as String).split(":")
      def toVersion = toGa.length > 2 ? toGa[2] : null
      container.rewrite(ga[0], ga[1], toGa[0], toGa[1], toVersion)
    }
  }
}
