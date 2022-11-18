package com.xebialabs.gradle.dependency.tasks

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigResolveOptions
import com.typesafe.config.ConfigValueFactory
import com.xebialabs.gradle.dependency.DependencyManagementContainer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject
import java.nio.file.Files

class ExportConfTask extends DefaultTask {
  static String emptyConfigTemplate = """\
dependencyManagement { 
  versions {} 
  dependencies: [] 
  blacklist: [] 
  rewrites {}
}
"""

  private final RegularFileProperty outputFile
  private final ConfigurableFileCollection inputFiles
  private DependencyManagementContainer dependencyManagementContainer
  private ProjectLayout projectLayout

  @Inject
  ExportConfTask(ObjectFactory objectFactory, ProjectLayout projectLayout) {
    this.projectLayout = projectLayout
    outputFile = objectFactory.fileProperty()
    inputFiles = objectFactory.fileCollection()
  }

  @Internal
  DependencyManagementContainer getDependencyManagementContainer() {
    return dependencyManagementContainer
  }

  void setDependencyManagementContainer(final DependencyManagementContainer dependencyManagementContainer) {
    this.dependencyManagementContainer = dependencyManagementContainer
  }

  @OutputFile
  RegularFileProperty getOutputFile() {
    outputFile
  }

  @InputFiles
  ConfigurableFileCollection getInputFiles() {
    inputFiles
  }

  void setInputFiles(Collection<File> suppliedConfigFiles) {
    inputFiles.setFrom(suppliedConfigFiles)
  }

  protected Config mergeConfigList(String path, Config c1, Config c2) {
    def v1 = c1.getList(path).unwrapped()
    def v2 = []
    if (c2.hasPath(path)) {
      v2 = c2.getList(path).unwrapped()
    }
    def newValue = ConfigValueFactory.fromIterable([*v1, *v2])
    c1.withValue(path, newValue)
  }

  protected Config mergeConfig(String path, Config c1, Config c2) {
    def v1 = c1.getConfig(path)
    if (c2.hasPath(path)) {
      def v2 = c2.getConfig(path)
      def newValue = v1.withFallback(v2)
      c1.withValue(path, newValue.root())
    } else {
      c1
    }
  }

  protected Config mergeConfigs(Config c1, Config c2) {
    def merged = mergeConfigList("dependencyManagement.dependencies", c1, c2)
    merged = mergeConfigList("dependencyManagement.blacklist", merged, c2)
    merged = mergeConfig("dependencyManagement.rewrites", merged, c2)
    merged = mergeConfig("dependencyManagement.versions", merged, c2)
    merged
  }

  @TaskAction
  void exportConf() {
    Config emptyConfig = ConfigFactory.parseString(emptyConfigTemplate)
    List<Config> configs = dependencyManagementContainer.getConfigs()
    Config mergedConfig = configs.findAll { !it.isEmpty()} inject(emptyConfig) { result, config ->
      mergeConfigs(result, config)
    }
    RegularFile outputConf = outputFile.get()
    File outputConfFile = outputConf.asFile
    outputConfFile.createNewFile()
    logger.info("Updating content of ${outputConfFile.path}")
    def renderedConfigs = [mergedConfig.with { config ->
      ConfigResolveOptions resolveOptions = ConfigResolveOptions.defaults().setAllowUnresolved(true)
      Config resolvedConfig = config.resolve(resolveOptions)
      ConfigRenderOptions renderOptions = ConfigRenderOptions.defaults().setFormatted(true).setComments(true).setJson(false).setOriginComments(false)
      resolvedConfig.root().render(renderOptions)
    }]
    Files.write(outputConfFile.toPath(), renderedConfigs)
  }

}
