package com.xebialabs.gradle.dependency

import com.xebialabs.gradle.dependency.supplier.ConfigSupplier
import com.xebialabs.gradle.dependency.supplier.DependencySupplier
import com.xebialabs.gradle.dependency.supplier.FileSupplier
import org.gradle.api.Project

class DependencyManagementExtension {

  DependencyManagementContainer container
  Project project

  DependencyManagementExtension(Project project, DependencyManagementContainer container) {
    this.project = project
    this.container = container
  }

  def supplier(ConfigSupplier dms) {
    container.addSupplier(dms)
  }

  def importConf(File f) {
    if (f.exists()) {
      project.logger.info("Added dependency management file: $f")
      container.addSupplier(new FileSupplier(f))
    } else {
      throw new FileNotFoundException("Cannot configure dependency management from non-existing file $f")
    }
  }

  def importConf(Map attrs) {
    String dependency = attrs.dependency
    String extension = attrs.extension ? attrs.extension : "conf"
    String classifier = attrs.classifier ? attrs.classifier : null
    importConf(dependency, extension, classifier)
  }

  def importConf(String dependency, String extension = "conf", String classifier = null) {
    project.logger.info("Added dependency management artifact: ${dependency}")
    container.addSupplier(new DependencySupplier(this.project, dependency, extension, classifier))
  }

  def useJavaPlatform(Boolean useJavaPlatform) {
    project.logger.warn("Dependency management plugin uses 'java-platform': $useJavaPlatform")
    container.useJavaPlatform = useJavaPlatform
  }
}
