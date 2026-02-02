package com.xebialabs.gradle.dependency.supplier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.typesafe.config.Config;

import com.xebialabs.gradle.dependency.DependencyManagementContainer;

public class MasterDependencyConfigSupplier extends DependencyManagementSupplier implements ConfigFileCollector {

    private final List<Config> configs = new ArrayList<>();
    private final Set<File> configFiles = new HashSet<>();

    @Override
    public void collectDependencies(DependencyManagementContainer container) {
        for (Config config : configs) {
            collectDependencies(config, container);
        }
    }

    @Override
    public void collectVersions(DependencyManagementContainer container) {
        for (Config config : configs) {
            collectVersions(config, container);
        }
    }

    @Override
    public void collectExclusions(DependencyManagementContainer container) {
        for (Config config : configs) {
            collectExclusions(config, container);
        }
    }

    @Override
    public void collectRewrites(DependencyManagementContainer container) {
        for (Config config : configs) {
            collectRewrites(config, container);
        }
    }

    public void addConfig(ConfigSupplier p) {
        configs.add(p.getConfig(this));
    }

    public List<Config> getConfigs() {
        return configs;
    }

    @Override
    public void collect(File configFile) {
        configFiles.add(configFile);
    }

    public Set<File> getSuppliedConfigFiles() {
        return configFiles;
    }
}
