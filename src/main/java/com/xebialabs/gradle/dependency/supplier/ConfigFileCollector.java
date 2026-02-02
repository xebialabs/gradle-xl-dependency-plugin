package com.xebialabs.gradle.dependency.supplier;

import java.io.File;

public interface ConfigFileCollector {
  void collect(File configFile);
}
