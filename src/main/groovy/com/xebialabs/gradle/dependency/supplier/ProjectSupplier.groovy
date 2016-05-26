package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.gradle.api.Project

class ProjectSupplier extends ConfigSupplier {

  public static final String VERSION_KEY_PREFIX = "dependencyManagement.versions."
  private Project project

  ProjectSupplier(Project project) {
    this.project = project
  }

  @Override
  Config getConfig() {
    def versions = project.properties.findAll { k, v -> k.startsWith(VERSION_KEY_PREFIX) }
    if (versions) {
      return ConfigFactory.parseMap(versions)
    } else {
      return ConfigFactory.empty()
    }
  }
}
