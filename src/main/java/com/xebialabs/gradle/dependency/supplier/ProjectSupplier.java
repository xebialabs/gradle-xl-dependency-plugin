package com.xebialabs.gradle.dependency.supplier;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

public class ProjectSupplier implements ConfigSupplier {

  public static final String VERSION_KEY_PREFIX = "dependencyManagement.versions.";
  
  private final Project project;

  public ProjectSupplier(Project project) {
    this.project = project;
  }

  @Override
  public Config getConfig(ConfigFileCollector collector) {
    Map<String, Object> versions = new HashMap<>();
    for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
      if (entry.getKey().startsWith(VERSION_KEY_PREFIX)) {
        versions.put(entry.getKey(), entry.getValue());
      }
    }
    
    if (!versions.isEmpty()) {
      return ConfigFactory.parseMap(versions);
    } else {
      return ConfigFactory.empty();
    }
  }
}
