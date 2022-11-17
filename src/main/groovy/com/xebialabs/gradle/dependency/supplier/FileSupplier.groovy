package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class FileSupplier implements ConfigSupplier {
  private File file
  private Config config

  FileSupplier(File file) {
    this.file = file
  }

  Config getConfig(ConfigFileCollector collector) {
    if (!config) {
      config = ConfigFactory.parseFile(file).resolve()
    }
    collector.collect(file)
    return config
  }
}
