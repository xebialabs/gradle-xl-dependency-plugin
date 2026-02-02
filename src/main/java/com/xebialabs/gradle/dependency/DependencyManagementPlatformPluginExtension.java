package com.xebialabs.gradle.dependency;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class DependencyManagementPlatformPluginExtension {
  
  private final Property<Boolean> restrictDependenciesEnabled;
  private final Property<Boolean> pinVersions;

  @Inject
  public DependencyManagementPlatformPluginExtension(ObjectFactory objectFactory) {
    // restrictDependenciesEnabled is very aggressive and might help troubleshoot conflicting constraints
    this.restrictDependenciesEnabled = objectFactory.property(Boolean.class).convention(false);
    // pinVersions is required if you want to use versions defined in the dependency management conf file
    // pinVersions will create version constraint for dependencies managed by com.xebialabs.dependency plugin
    this.pinVersions = objectFactory.property(Boolean.class).convention(true);
  }

  public Property<Boolean> getRestrictDependenciesEnabled() {
    return restrictDependenciesEnabled;
  }

  public Property<Boolean> getPinVersions() {
    return pinVersions;
  }
}
