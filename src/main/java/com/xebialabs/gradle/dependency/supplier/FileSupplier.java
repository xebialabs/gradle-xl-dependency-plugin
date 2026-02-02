package com.xebialabs.gradle.dependency.supplier;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class FileSupplier implements ConfigSupplier {
  
  private final File file;
  private Config config;

  public FileSupplier(File file) {
    this.file = file;
  }

  @Override
  public Config getConfig(ConfigFileCollector collector) {
    if (config == null) {
      config = ConfigFactory.parseFile(file).resolve();
    }
    collector.collect(file);
    return config;
  }
}
