package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.LibraryElements

class DependencySupplier implements ConfigSupplier {

  private String dependency
  private Config config
  private Project project
  private String extension
  private String classifier
  private String suffix

  DependencySupplier(Project project, String dependency, String extension, String classifier) {
    this.project = project
    this.dependency = dependency
    this.extension = extension
    this.classifier = classifier
    this.suffix = createSuffix(classifier, extension)
  }

  private String createSuffix(String classifier, String extension) {
    String suffix = ""
    if (classifier?.trim()) {
      suffix = ":$classifier"
    }
    if (extension?.trim()) {
      suffix = suffix + "@$extension"
    }
    return suffix
  }

  Config getConfig(ConfigFileCollector collector) {
    if (!config) {
      Dependency dep = project.dependencies.create(dependency + suffix)
      def xlRefConf = project.configurations.detachedConfiguration(dep)
      xlRefConf.attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements, 'conf-file'))
      }
      xlRefConf.setTransitive(false)
      def resolvedFile = xlRefConf.singleFile
      collector.collect(resolvedFile)
      assert resolvedFile.exists(): "Dependency ${dependency} was not resolved into a file"
      config = ConfigFactory.parseFile(resolvedFile).resolve()
    }
    return config
  }
}
