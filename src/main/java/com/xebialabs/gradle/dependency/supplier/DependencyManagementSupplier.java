package com.xebialabs.gradle.dependency.supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.typesafe.config.*;

import com.xebialabs.gradle.dependency.DependencyManagementContainer;

public abstract class DependencyManagementSupplier {

    public abstract void collectDependencies(DependencyManagementContainer container);

    public abstract void collectVersions(DependencyManagementContainer container);

    public abstract void collectExclusions(DependencyManagementContainer container);

    public abstract void collectRewrites(DependencyManagementContainer container);

    protected void collectDependencies(Config config, DependencyManagementContainer container) {
        if (config.hasPath("dependencyManagement.dependencies")) {
            parseDependencies(config.getList("dependencyManagement.dependencies"), container);
        }
    }

    protected void collectVersions(Config config, DependencyManagementContainer container) {
        if (config.hasPath("dependencyManagement.versions")) {
            parseVersions(config.getConfig("dependencyManagement.versions"), container);
        }
    }

    protected void collectExclusions(Config config, DependencyManagementContainer container) {
        if (config.hasPath("dependencyManagement.blacklist")) {
            parseBlackList(config.getList("dependencyManagement.blacklist"), container);
        }
    }

    protected void collectRewrites(Config config, DependencyManagementContainer container) {
        if (config.hasPath("dependencyManagement.rewrites")) {
            parseRewrites(config.getConfig("dependencyManagement.rewrites"), container);
        }
    }

    private void parseVersions(Config config, DependencyManagementContainer dependencyManagementContainer) {
        for (Map.Entry<String, ConfigValue> e : config.entrySet()) {
            String key = e.getKey();
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            dependencyManagementContainer.registerVersionKey(key, (String) e.getValue().unwrapped());
        }
    }

    private void parseDependencies(ConfigList list, DependencyManagementContainer container) {
        for (ConfigValue v : list) {
            if (v.valueType() == ConfigValueType.STRING) {
                String[] gav = ((String) v.unwrapped()).split("[:@]");
                container.addManagedVersion(gav[0], gav[1], gav[2]);
            } else if (v.valueType() == ConfigValueType.OBJECT) {
                ConfigObject o = (ConfigObject) v;
                String group = (String) o.get("group").unwrapped();
                String version = (String) o.get("version").unwrapped();
                ConfigList emptyExcludes = ConfigValueFactory.fromIterable(new ArrayList<>());

                if (o.containsKey("artifacts")) {
                    ConfigList excludesList = (ConfigList) o.getOrDefault("excludes", emptyExcludes);
                    List<String> excludes = new ArrayList<>();
                    for (ConfigValue entry : excludesList) {
                        excludes.add((String) entry.unwrapped());
                    }

                    ConfigList artifacts = (ConfigList) o.get("artifacts");
                    for (ConfigValue entry : artifacts) {
                        String artifact = (String) entry.unwrapped();
                        container.addManagedVersion(group, artifact, version, excludes);
                    }
                } else {
                    String artifact = (String) o.get("artifact").unwrapped();
                    ConfigList excludesList = (ConfigList) o.getOrDefault("excludes", emptyExcludes);
                    List<String> excludes = new ArrayList<>();
                    for (ConfigValue entry : excludesList) {
                        excludes.add((String) entry.unwrapped());
                    }
                    container.addManagedVersion(group, artifact, version, excludes);
                }
            }
        }
    }

    private void parseBlackList(ConfigList list, DependencyManagementContainer container) {
        for (ConfigValue cv : list) {
            String[] ga = ((String) cv.unwrapped()).split(":");
            if (ga.length == 1) {
                container.blackList(ga[0], null);
            } else {
                container.blackList(ga[0], ga[1]);
            }
        }
    }

    private void parseRewrites(Config config, DependencyManagementContainer container) {
        for (Map.Entry<String, ConfigValue> e : config.entrySet()) {
            String key = e.getKey();
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            String[] ga = key.split(":");
            String[] toGa = ((String) e.getValue().unwrapped()).split(":");
            container.rewrite(ga[0], ga[1], toGa[0], toGa[1]);
        }
    }
}
