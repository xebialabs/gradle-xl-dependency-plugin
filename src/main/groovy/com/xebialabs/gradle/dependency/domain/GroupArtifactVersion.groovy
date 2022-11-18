package com.xebialabs.gradle.dependency.domain

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.gradle.api.artifacts.ModuleVersionSelector

@CompileStatic
@EqualsAndHashCode
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
  String toString() {
    return "GroupArtifactVersion($group:$artifact:$version)"
  }
}
