package com.xebialabs.gradle.dependency

import com.typesafe.config.Config
import com.xebialabs.gradle.dependency.domain.GroupArtifact
import com.xebialabs.gradle.dependency.supplier.ConfigSupplier
import com.xebialabs.gradle.dependency.supplier.MasterDependencyConfigSupplier
import groovy.text.SimpleTemplateEngine
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class DependencyManagementContainer implements DependencyResolutionListener {
  private static final Logger logger = Logging.getLogger(DependencyManagementContainer.class)

  private SimpleTemplateEngine engine = new SimpleTemplateEngine()
  private MasterDependencyConfigSupplier supplier = new MasterDependencyConfigSupplier()
  private List<Project> projects = []
  private Project rootProject = null
  private boolean resolved = false

  // useJavaPlatform - false is pre gradle 7 style of managing dependencies
  boolean useJavaPlatform = false

  // versions MUST be valid simple groovy template string keys (i.e. cannot be 'something.nested.property')
  Map versions = [:].withDefault { "" }

  Map<String, String> resolveCache = [:].withDefault { String s ->
    Map resolutionContextMap = (versions + rootProject.extensions.extraProperties.properties).withDefault { "" }
    s ? engine.createTemplate(s).make(resolutionContextMap).toString() : s
  }
  Map managedVersions = [:]
  Map<String, List<String>> managedExcludes = new HashMap<>()
  List<GroupArtifact> blackList = []
  Map rewrites = [:]

  DependencyManagementContainer(Project project) {
    this.rootProject = project
    this.rootProject.gradle.addListener(this)
    projects.addAll(project.allprojects)
  }

  def resolveIfNecessary() {
    if (!resolved) {
      logger.info("Resolving project management plugin container configuration")
      this.supplier.collectDependencies(this)
      // this.supplier.collectRewrites(this) - loaded when created
      resolved = true
      configureProjects()
    }
  }

  private def configureProjects() {
    // once container is resolved we can configure rewrites - ONCE
    projects.each { Project p ->
      DependencyManagementProjectConfigurer.configureRewrites(this, p)
      DependencyManagementProjectConfigurer.configureExcludeRules(this, p)
    }
  }

  def addSupplier(ConfigSupplier supplier) {
    this.supplier.addConfig(supplier)
    this.supplier.collectVersions(this)
    // WE want to collect the exclusions late, however that somehow does not work.
    this.supplier.collectExclusions(this)
    this.supplier.collectRewrites(this)
    exposeVersions() // versions should be exposed via registerVersionKey
    resolved = false
  }

  private def exposeVersions() {
    versions << versions.collectEntries { k, v ->
      if (k !== "out") {
        if (rootProject.extensions.extraProperties.has(k)) {
          def overrideValue = rootProject.extensions.extraProperties.get(k)
          logger.info("${rootProject.name} added overriden version ${k}=${overrideValue} (found in gradle extra properties)")
          rootProject.extensions.extraProperties.set(k, overrideValue)
          [k:overrideValue]
        } else {
          if (v !== "") {
            logger.info("${rootProject.name} added version $k=$v")
            rootProject.extensions.extraProperties.set(k, v)
            [k:v]
          } else {
            logger.error("Unable to expose version ${k} to gradle extra properties. Check if it is defined in gradle.properties or .conf file")
            [k:""]
          }
        }
      } else {
        [:]
      }
    }
  }

  def registerVersionKey(String key, String version) {
    if (!versions[key]) {
      versions.put(key, version)
      logger.debug("Registering version $key = $version")
      // Also register the version key on each project, useful with for example $scalaVersion
      projects.each {
        it.extensions.extraProperties.set(key, version)
      }
    }
  }

  def addManagedVersion(String group, String artifact, String version) {
    addManagedVersion(group, artifact, version, [])
  }

  def addManagedVersion(String group, String artifact, String version, List<String> excludes) {
    def ga = resolve("$group:$artifact")
    def resolvedVersion = resolve(version)
    logger.debug("Adding managed version $ga -> $resolvedVersion, excludes: $excludes")
    managedVersions[ga] = resolvedVersion
    def oldValue = managedExcludes.getOrDefault(ga, [])
    oldValue.addAll(excludes)
    if (!oldValue.empty) {
      managedExcludes.put(ga, oldValue)
    }
  }

  String getManagedVersion(String group, String artifact) {
    String ga = resolve("$group:$artifact")
    logger.debug("Trying to resolve version for $ga")
    if (managedVersions[ga]) {
      return managedVersions[ga]
    } else {
      logger.debug("Unable to find $ga in $managedVersions")
    }
    return null
  }

  def resolve(String s) {
    return resolveCache.get(s)
  }

  def blackList(String group, String artifact) {
    def ga = new GroupArtifact(resolve(group), resolve(artifact))
    blackList.add(ga)
  }

  def rewrite(String fromGroup, String fromArtifact, String toGroup, String toArtifact) {
    def fromGa = new GroupArtifact(fromGroup, fromArtifact)
    def toGa = new GroupArtifact(toGroup, toArtifact)
    this.rewrites.put(fromGa, toGa)
  }

  @Override
  void beforeResolve(final ResolvableDependencies dependencies) {
    this.resolveIfNecessary()
  }

  @Override
  void afterResolve(final ResolvableDependencies dependencies) {
    // nothing
  }

  List<Config> getConfigs() {
    return supplier.getConfigs()
  }

  Set<File> getSuppliedConfigFiles() {
    return supplier.getSuppliedConfigFiles()
  }
}
