package com.xebialabs.gradle.dependency.domain;

import org.gradle.api.artifacts.ModuleVersionSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GroupArtifactVersion extends GroupArtifact {
  
  private final String version;

  public GroupArtifactVersion(String group, String artifact, String version) {
    super(group, artifact);
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public Map<String, String> toMap(ModuleVersionSelector selector) {
    Map<String, String> map = new HashMap<>();
    map.put("group", getGroup());
    map.put("name", getArtifact());
    map.put("version", version);
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    GroupArtifactVersion that = (GroupArtifactVersion) o;
    return Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "GroupArtifactVersion(" + getGroup() + ":" + getArtifact() + ":" + version + ")";
  }
}
