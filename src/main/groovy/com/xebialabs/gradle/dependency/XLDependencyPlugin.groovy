package com.xebialabs.gradle.dependency

import com.xebialabs.gradle.dependency.supplier.ProjectSupplier
import org.gradle.api.Plugin
import org.gradle.api.Project

class XLDependencyPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.apply plugin: "com.xebialabs.dependency.base"

    def defaultConfFile = project.file("gradle/dependencies.conf")

    project.dependencyManagement {
      supplier new ProjectSupplier(project)
      if (defaultConfFile.exists()) {
        importConf defaultConfFile
      }
    }

  }
}
