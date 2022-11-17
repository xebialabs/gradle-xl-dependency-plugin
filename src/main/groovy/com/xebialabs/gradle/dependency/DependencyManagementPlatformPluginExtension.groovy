package com.xebialabs.gradle.dependency

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

abstract class DependencyManagementPlatformPluginExtension {
  Property<Boolean> restrictDependenciesEnabled
  Property<Boolean> pinVersions

  @Inject
  DependencyManagementPlatformPluginExtension(ObjectFactory objectFactory) {
    // restrictDependenciesEnabled is very aggressive and might help troubleshoot conflicting constraints
    restrictDependenciesEnabled = objectFactory.property(Boolean).convention(false)
    // pinVersions is required if you want to use versions defined in the dependency management conf file
    // pinVersions will create version constraint for dependencies managed by com.xebialabs.dependency plugin
    pinVersions = objectFactory.property(Boolean).convention(true)
  }
}
