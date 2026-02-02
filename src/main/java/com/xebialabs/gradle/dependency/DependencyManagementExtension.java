package com.xebialabs.gradle.dependency;

import com.xebialabs.gradle.dependency.supplier.ConfigSupplier;
import com.xebialabs.gradle.dependency.supplier.DependencySupplier;
import com.xebialabs.gradle.dependency.supplier.FileSupplier;
import org.gradle.api.Project;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

public class DependencyManagementExtension {

  private final DependencyManagementContainer container;
  private final Project project;

  public DependencyManagementExtension(Project project, DependencyManagementContainer container) {
    this.project = project;
    this.container = container;
  }

  public DependencyManagementContainer getContainer() {
    return container;
  }

  public Project getProject() {
    return project;
  }

  public void supplier(ConfigSupplier dms) {
    container.addSupplier(dms);
  }

  public void importConf(File f) throws FileNotFoundException {
    if (f.exists()) {
      project.getLogger().info("Added dependency management file: " + f);
      container.addSupplier(new FileSupplier(f));
    } else {
      throw new FileNotFoundException("Cannot configure dependency management from non-existing file " + f);
    }
  }

  public void importConf(Map<String, ?> attrs) {
    String dependency = String.valueOf(attrs.get("dependency"));
    String extension = attrs.containsKey("extension") ? String.valueOf(attrs.get("extension")) : "conf";
    String classifier = attrs.containsKey("classifier") ? String.valueOf(attrs.get("classifier")) : null;
    importConf(dependency, extension, classifier);
  }

  public void importConf(String dependency) {
    importConf(dependency, "conf", null);
  }

  public void importConf(String dependency, String extension) {
    importConf(dependency, extension, null);
  }

  public void importConf(String dependency, String extension, String classifier) {
    project.getLogger().info("Added dependency management artifact: " + dependency);
    container.addSupplier(new DependencySupplier(this.project, dependency, extension, classifier));
  }

  public void useJavaPlatform(Boolean useJavaPlatform) {
    project.getLogger().warn("Dependency management plugin uses 'java-platform': " + useJavaPlatform);
    container.setUseJavaPlatform(useJavaPlatform);
  }
}
