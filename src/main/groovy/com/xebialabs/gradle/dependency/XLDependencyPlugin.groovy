package com.xebialabs.gradle.dependency

import com.xebialabs.gradle.dependency.supplier.ProjectSupplier
import org.gradle.api.Plugin
import org.gradle.api.Project

class XLDependencyPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.apply plugin: "xebialabs.dependency.base"

    project.dependencyManagement {
      supplier new ProjectSupplier(project)
      importConf project.file("gradle/dependencies.conf")
    }

  }
}
