package com.xebialabs.gradle.dependency

import com.xebialabs.gradle.dependency.domain.GroupArtifact
import com.xebialabs.gradle.dependency.supplier.ConfigSupplier
import com.xebialabs.gradle.dependency.supplier.MasterDependencyConfigSupplier
import groovy.text.SimpleTemplateEngine
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtraPropertiesExtension

class DependencyManagementContainer {
  private static final Logger logger = Logging.getLogger(DependencyManagementContainer.class)

  private SimpleTemplateEngine engine = new SimpleTemplateEngine()
  private MasterDependencyConfigSupplier supplier = new MasterDependencyConfigSupplier()
  private List<Project> projects = []
  private Project rootProject = null
  private boolean resolved = false

  boolean manageDependencies = true

  Map versions = [:].withDefault { "" }

  Map<String, String> resolveCache = [:].withDefault { String s ->
    Map resolutionContextMap = (versions + rootProject.extensions.extraProperties.properties).withDefault { "" }
    s ? engine.createTemplate(s).make(resolutionContextMap).toString() : s
  }
  Map managedVersions = [:]
  List<GroupArtifact> blackList = []
  Map rewrites = [:]

  DependencyManagementContainer(Project project) {
    this.rootProject = project
    projects.addAll(project.allprojects)
  }

  def resolveIfNecessary() {
    if (!resolved) {
      this.supplier.collectDependencies(this)
      this.supplier.collectRewrites(this)
      resolved = true
    }
  }

  def addSupplier(ConfigSupplier supplier) {
    this.supplier.addConfig(supplier)
    this.supplier.collectVersions(this)
    // WE want to collect the exclusions late, however that somehow does not work.
    this.supplier.collectExclusions(this)
    exposeVersions()
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
        it.extensions.findByType(ExtraPropertiesExtension).set(key, version)
      }
    }
  }

  def addManagedVersion(String group, String artifact, String version) {
    def ga = resolve("$group:$artifact")
    def resolvedVersion = resolve(version)
    logger.debug("Adding managed version $ga -> $resolvedVersion")
    managedVersions[ga] = resolvedVersion
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
}
