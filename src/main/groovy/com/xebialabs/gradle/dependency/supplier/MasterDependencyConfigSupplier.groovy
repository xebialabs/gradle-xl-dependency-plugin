package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.xebialabs.gradle.dependency.DependencyManagementContainer

public class MasterDependencyConfigSupplier extends DependencyManagementSupplier implements ConfigFileCollector {
    private List<Config> configs = []
    private Set<File> configFiles = []

    @Override
    def collectDependencies(DependencyManagementContainer container) {
        configs.each {
          collectDependencies(it, container)
        }
    }

    @Override
    def collectVersions(DependencyManagementContainer container) {
      configs.each {
        collectVersions(it, container)
      }
    }

    @Override
    def collectExclusions(DependencyManagementContainer container) {
      configs.each {
        collectExclusions(it, container)
      }
    }

    @Override
    def collectRewrites(DependencyManagementContainer container) {
      configs.each {
        collectRewrites(it, container)
      }
    }

    def addConfig(ConfigSupplier p) {
       configs.add(p.getConfig(this))
    }

    List<Config> getConfigs() {
      configs
    }

    @Override
    void collect(final File configFile) {
      configFiles.add(configFile)
    }

    Set<File> getSuppliedConfigFiles() {
      return configFiles
    }
}
