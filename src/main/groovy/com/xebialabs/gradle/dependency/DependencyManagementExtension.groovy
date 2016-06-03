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

  def importConf(String s) {
    project.logger.info("Added dependency management artifact: $s")
    container.addSupplier(new DependencySupplier(this.project, s))
  }
}
