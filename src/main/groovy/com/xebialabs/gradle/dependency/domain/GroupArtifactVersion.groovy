package com.xebialabs.gradle.dependency.domain

import org.gradle.api.artifacts.ModuleVersionSelector

class GroupArtifactVersion extends GroupArtifact {
  String version

  GroupArtifactVersion(String group, String artifact, String version) {
    super(group, artifact)
    this.version = version
  }

  @Override
  Map toMap(ModuleVersionSelector selector) {
    return [group: group, name: artifact, version: version]
  }

  @Override
  public String toString() {
    return "GroupArtifactVersion($group:$artifact:$version)"
  }
}
