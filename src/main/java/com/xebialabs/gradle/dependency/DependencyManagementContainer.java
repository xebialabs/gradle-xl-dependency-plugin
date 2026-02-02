package com.xebialabs.gradle.dependency;

import com.typesafe.config.Config;
import com.xebialabs.gradle.dependency.domain.GroupArtifact;
import com.xebialabs.gradle.dependency.domain.GroupArtifactVersion;
import com.xebialabs.gradle.dependency.supplier.ConfigSupplier;
import com.xebialabs.gradle.dependency.supplier.MasterDependencyConfigSupplier;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyResolutionListener;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.util.*;

public class DependencyManagementContainer implements DependencyResolutionListener {
  
  private static final Logger logger = Logging.getLogger(DependencyManagementContainer.class);

  private final MasterDependencyConfigSupplier supplier = new MasterDependencyConfigSupplier();
  private final List<Project> projects = new ArrayList<>();
  private Project rootProject = null;
  private boolean resolved = false;

  // useJavaPlatform - false is pre gradle 7 style of managing dependencies
  private boolean useJavaPlatform = false;

  // versions MUST be valid simple groovy template string keys (i.e. cannot be 'something.nested.property')
  private final Map<String, String> versions = new HashMap<>();

  // Resolution context map with default value behavior
  private final Map<String, String> resolutionContextMap;

  // Resolve cache with template resolution
  private final Map<String, String> resolveCache;
  
  private final Map<String, String> managedVersions = new HashMap<>();
  private final Map<String, List<String>> managedExcludes = new HashMap<>();
  private final List<GroupArtifact> blackList = new ArrayList<>();
  private final Map<GroupArtifact, GroupArtifact> rewrites = new HashMap<>();

  private final Set<GroupArtifactVersion> unresolvedDependencies = new HashSet<>();

  public DependencyManagementContainer(Project project) {
    this.rootProject = project;
    this.rootProject.getGradle().addListener(this);
    this.projects.addAll(project.getAllprojects());
    this.resolutionContextMap = initializeResolutionContext();
    this.resolveCache = new DefaultValueMap<>(s -> 
      s != null && !s.isEmpty() ? SimpleTemplateResolver.resolve(s, resolutionContextMap) : s
    );
  }

  public void resolveIfNecessary() {
    if (!resolved) {
      logger.info("Resolving project management plugin container configuration");
      // in a composite build this method may be called from the parent build
      // in that case versions,excludes and blacklists would not be collected
      this.supplier.collectVersions(this);
      exposeVersions(); // versions should be exposed via registerVersionKey
      // WE want to collect the exclusions late, however that somehow does not work.
      this.supplier.collectExclusions(this);
      this.supplier.collectRewrites(this);
      this.supplier.collectDependencies(this);
      // this.supplier.collectRewrites(this) - loaded when created
      configureProjects();
      resolved = true;
    }
  }

  private void configureProjects() {
    // once container is resolved we can configure rewrites - ONCE
    for (Project p : projects) {
      DependencyManagementProjectConfigurer.configureRewrites(this, p);
      DependencyManagementProjectConfigurer.configureExcludeRules(this, p);
    }
  }

  public void addSupplier(ConfigSupplier supplier) {
    this.supplier.addConfig(supplier);
    this.supplier.collectVersions(this);
    exposeVersions(); // versions should be exposed via registerVersionKey
    resolved = false;
  }

  private void exposeVersions() {
    // this looks like a duplicate of registerVersionKey
    for (Map.Entry<String, String> entry : versions.entrySet()) {
      String k = entry.getKey();
      String v = entry.getValue();
      if (v != null && !v.isEmpty() && !k.equals("out")) {
        logger.debug(rootProject.getPath() + " added " + k + "=" + v);
        rootProject.getExtensions().getExtraProperties().set(k, v);
      }
    }
  }

  public void registerVersionKey(String key, String version) {
    String previousValue = versions.get(key);
    boolean isNewVersion = previousValue == null && !key.equals(previousValue);
    if (isNewVersion) {
      versions.put(key, version);
      // Also register the version key on each project, useful with for example $scalaVersion
      rootProject.getExtensions().getExtraProperties().set(key, version);
    }
  }

  public void addManagedVersion(String group, String artifact, String version) {
    addManagedVersion(group, artifact, version, Collections.emptyList());
  }

  public void addManagedVersion(String group, String artifact, String version, List<String> excludes) {
    unresolvedDependencies.add(new GroupArtifactVersion(group, artifact, version));
    String ga = resolve(group + ":" + artifact);
    String resolvedVersion = resolve(version);
    logger.debug("Adding managed version " + ga + " -> " + resolvedVersion + ", excludes: " + excludes);
    managedVersions.put(ga, resolvedVersion);

    List<String> oldValue = managedExcludes.getOrDefault(ga, new ArrayList<>());
    oldValue.addAll(excludes);
    if (!oldValue.isEmpty()) {
      managedExcludes.put(ga, oldValue);
    }
  }

  public String getManagedVersion(String group, String artifact) {
    String ga = resolve(group + ":" + artifact);
    logger.debug("Trying to resolve version for " + ga);
    if (managedVersions.containsKey(ga)) {
      return managedVersions.get(ga);
    } else {
      logger.debug("Unable to find " + ga + " in " + managedVersions);
    }
    return null;
  }

  public String resolve(String s) {
    return resolveCache.get(s);
  }

  public void blackList(String group, String artifact) {
    GroupArtifact ga = new GroupArtifact(resolve(group), resolve(artifact));
    blackList.add(ga);
  }

  public void rewrite(String fromGroup, String fromArtifact, String toGroup, String toArtifact) {
    GroupArtifact fromGa = new GroupArtifact(fromGroup, fromArtifact);
    GroupArtifact toGa = new GroupArtifact(toGroup, toArtifact);
    this.rewrites.put(fromGa, toGa);
  }

  @Override
  public void beforeResolve(ResolvableDependencies dependencies) {
    // resolution of dependencies will happen even when we ask for the conf file itself - i.e. while we're building resolution context
    var confFileAttribute = dependencies.getAttributes().getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE);
    boolean isNotConfFile = confFileAttribute == null;
    // this is weird, but we should resolve only if we were not asked to provide conf file - ie for something else
    if (isNotConfFile) {
      this.resolveIfNecessary();
    }
  }

  @Override
  public void afterResolve(ResolvableDependencies dependencies) {
    // nothing
  }

  private Map<String, String> initializeResolutionContext() {
    return new DefaultValueMap<>(k -> {
      String contextValue = "${" + k + "}";
      String versionValue = versions.get(k);
      Object extValue = rootProject.getExtensions().getExtraProperties().getProperties().get(k);
      
      if (versionValue != null) {
        contextValue = versionValue;
      } else if (extValue != null) {
        contextValue = extValue.toString();
      }
      return contextValue;
    });
  }

  public List<Config> getConfigs() {
    return supplier.getConfigs();
  }

  public Set<File> getSuppliedConfigFiles() {
    return supplier.getSuppliedConfigFiles();
  }

  // Getters for fields accessed by other classes
  public boolean isUseJavaPlatform() {
    return useJavaPlatform;
  }

  public void setUseJavaPlatform(boolean useJavaPlatform) {
    this.useJavaPlatform = useJavaPlatform;
  }

  public Map<String, String> getManagedVersions() {
    return managedVersions;
  }

  public Map<String, List<String>> getManagedExcludes() {
    return managedExcludes;
  }

  public List<GroupArtifact> getBlackList() {
    return blackList;
  }

  public Map<GroupArtifact, GroupArtifact> getRewrites() {
    return rewrites;
  }

  public Set<GroupArtifactVersion> getUnresolvedDependencies() {
    return unresolvedDependencies;
  }

  public Project getRootProject() {
    return rootProject;
  }
}
