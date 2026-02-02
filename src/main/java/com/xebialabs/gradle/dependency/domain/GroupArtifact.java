package com.xebialabs.gradle.dependency.domain;

import org.gradle.api.artifacts.ModuleVersionSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GroupArtifact {

  private final String group;
  private final String artifact;

  public GroupArtifact(String group, String artifact) {
    this.group = group;
    this.artifact = artifact;
  }

  public String getGroup() {
    return group;
  }

  public String getArtifact() {
    return artifact;
  }

  public GroupArtifactVersion withVersion(String version) {
    return new GroupArtifactVersion(group, artifact, version);
  }

  public Map<String, String> toMap(ModuleVersionSelector selector) {
    Map<String, String> map = new HashMap<>();
    if (artifact != null && !artifact.isEmpty()) {
      map.put("group", group);
      map.put("name", artifact);
      map.put("version", selector.getVersion());
    } else {
      map.put("group", group);
      map.put("name", selector.getName());
      map.put("version", selector.getVersion());
    }
    return map;
  }

  public Map<String, String> toMap() {
    Map<String, String> map = new HashMap<>();
    if (artifact != null && !artifact.isEmpty()) {
      map.put("group", group);
      map.put("module", artifact);
    } else {
      map.put("group", group);
    }
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GroupArtifact that = (GroupArtifact) o;

    if (!Objects.equals(artifact, that.artifact)) return false;
    return Objects.equals(group, that.group);
  }

  @Override
  public int hashCode() {
    int result = group != null ? group.hashCode() : 0;
    result = 31 * result + (artifact != null ? artifact.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "GroupArtifact(" + group + ":" + artifact + ")";
  }
}
