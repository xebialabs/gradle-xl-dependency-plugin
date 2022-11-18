package com.xebialabs.gradle.dependency

import com.typesafe.config.Config
import com.xebialabs.gradle.dependency.domain.GroupArtifact
import com.xebialabs.gradle.dependency.domain.GroupArtifactVersion
import com.xebialabs.gradle.dependency.supplier.ConfigSupplier
import com.xebialabs.gradle.dependency.supplier.MasterDependencyConfigSupplier
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class DependencyManagementContainer implements DependencyResolutionListener {
  private static final Logger logger = Logging.getLogger(DependencyManagementContainer.class)

  private SimpleTemplateEngine engine = new SimpleTemplateEngine()
  private MasterDependencyConfigSupplier supplier = new MasterDependencyConfigSupplier()
  private List<Project> projects = []
  private Project rootProject = null
  private boolean resolved = false

  // useJavaPlatform - false is pre gradle 7 style of managing dependencies
  boolean useJavaPlatform = false

  int count = 0

  // versions MUST be valid simple groovy template string keys (i.e. cannot be 'something.nested.property')
  Map<String, String> versions = new HashMap<String, String>() //.withDefault { it }

  // this code is not optimal at all - we create these maps for every missing key - i.e. a lot
  private final Map<String, String> resolutionContextMap

  final Map<String, String> resolveCache
  Map<String, String> managedVersions = [:]
  Map<String, List<String>> managedExcludes = [:]
  List<GroupArtifact> blackList = []
  Map<GroupArtifact, ? extends GroupArtifact> rewrites = [:]

  Set<GroupArtifactVersion> unresolvedDependencies = []

  DependencyManagementContainer(Project project) {
    this.rootProject = project
    this.rootProject.gradle.addListener(this)
    projects.addAll(project.allprojects)
    this.resolutionContextMap = initializeResolutionContext()
    resolveCache = new HashMap<String, String>().withDefault { String s ->
      count++
      s ? engine.createTemplate(s).make(resolutionContextMap).toString() : s
    }
  }

  def resolveIfNecessary() {
    if (!resolved) {
      logger.info("Resolving project management plugin container configuration")
      // in a composite build this method may be called from the parent build
      // in that case versions,excludes and blacklists would not be collected
      this.supplier.collectVersions(this)
      exposeVersions() // versions should be exposed via registerVersionKey
      // WE want to collect the exclusions late, however that somehow does not work.
      this.supplier.collectExclusions(this)
      this.supplier.collectRewrites(this)
      this.supplier.collectDependencies(this)
      // this.supplier.collectRewrites(this) - loaded when created
      configureProjects()
      resolved = true
      logger.error("resolveCache invoked $count times")
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
    exposeVersions() // versions should be exposed via registerVersionKey
    resolved = false
  }

  private def exposeVersions() {
    // this looks like a duplicate of registerVersionKey
    versions.collect { k, v ->
      if (v !== "" && k != "out") {
        logger.debug("${rootProject.path} added $k=$v")
        rootProject.extensions.extraProperties.set(k, v)
      }
    }
  }

  def registerVersionKey(String key, String version) {
    String previousValue = versions[key]
    boolean isNewVersion = !previousValue && previousValue != key
    if (isNewVersion) {
      versions.put(key, version)
      // Also register the version key on each project, useful with for example $scalaVersion
      rootProject.extensions.extraProperties.set(key, version)
//      projects.each {
//        logger.debug("${it.path} Registering version $key = $version")
//        it.extensions.extraProperties.set(key, version)
//      }
    }
  }

  def addManagedVersion(String group, String artifact, String version) {
    addManagedVersion(group, artifact, version, [])
  }

  def addManagedVersion(String group, String artifact, String version, List<String> excludes) {
    unresolvedDependencies << new GroupArtifactVersion(group, artifact, version)
    def ga = resolve("$group:$artifact")
    def resolvedVersion = resolve(version)
    logger.debug("Adding managed version $ga -> $resolvedVersion, excludes: $excludes")
    managedVersions[ga] = resolvedVersion

    def oldValue = managedExcludes.getOrDefault(ga, [])
    oldValue.addAll(excludes)
    if (!oldValue.empty) {
      managedExcludes.put(ga.toString(), oldValue)
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

  String resolve(String s) {
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
    // resolution of dependencies will happen even when we ask for the conf file itself - i.e. while we're building resolution context
    def confFileAttribute = dependencies.attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)
    def isNotConfFile = confFileAttribute == null
    // this is weird, but we should resolve only if we were not asked to provide conf file - ie for something else
    if (isNotConfFile) {
      this.resolveIfNecessary()
    }
  }

  @Override
  void afterResolve(final ResolvableDependencies dependencies) {
    // nothing
  }

  private Map<String, String> initializeResolutionContext() {
    return new HashMap<String, String>().withDefault { String k ->
      String contextValue = "\${$k}"
      def ext = rootProject.extensions.extraProperties.properties
      String versionValue = versions.get(k)
      String extValue = rootProject.extensions.extraProperties.properties.get(k)
      if (versionValue) {
        contextValue = versionValue
      } else if (extValue) {
        contextValue = extValue
      }
      return contextValue
    }
  }

  List<Config> getConfigs() {
    return supplier.getConfigs()
  }

  Set<File> getSuppliedConfigFiles() {
    return supplier.getSuppliedConfigFiles()
  }
}
