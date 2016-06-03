package com.xebialabs.gradle.dependency.supplier;

import com.typesafe.config.Config;
import com.xebialabs.gradle.dependency.DependencyManagementContainer;

import java.util.ArrayList;
import java.util.List;

public class MasterDependencyConfigSupplier extends DependencyManagementSupplier {
    private List<Config> configs = []

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
       configs.add(p.config)
    }
}
