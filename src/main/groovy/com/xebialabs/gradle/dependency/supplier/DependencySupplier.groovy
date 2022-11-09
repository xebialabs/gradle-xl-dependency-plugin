package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements

class DependencySupplier implements ConfigSupplier {

  private String dependency
  private Config config
  private Project project

  DependencySupplier(Project project, String dependency) {
    this.project = project
    this.dependency = dependency
  }

  Config getConfig() {
    if (!config) {
      def dependency = project.dependencies.create(dependency + "@conf")
      def xlRefConf = project.configurations.detachedConfiguration(dependency)
      xlRefConf.attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements, 'conf-file'))
      }
      xlRefConf.setTransitive(false)
      def resolvedFile = xlRefConf.singleFile

      assert resolvedFile.exists(): "Dependency ${dependency} was not resolved into a file"
      config = ConfigFactory.parseFile(resolvedFile).resolve()
    }
    return config
  }
}
