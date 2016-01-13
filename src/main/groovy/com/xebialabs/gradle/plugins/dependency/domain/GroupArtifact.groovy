package com.xebialabs.gradle.plugins.dependency.domain

import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector

class GroupArtifact {

    String group

    String artifact

    GroupArtifact(String group, String artifact) {
        this.group = group
        this.artifact = artifact
    }

    Map toMap(ModuleVersionSelector selector) {
        if (artifact) {
            return [group: group, name: artifact, version: selector.version]
        } else {
            return [group: group, name: selector.name, version: selector.version]
        }
    }

    Map toMap() {
        if (artifact) {
            return [group: group, module: artifact]
        } else {
            return [group: group]
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        GroupArtifact that = (GroupArtifact) o

        if (artifact != that.artifact) return false
        if (group != that.group) return false

        return true
    }

    int hashCode() {
        int result
        result = group.hashCode()
        result = 31 * result + (artifact != null ? artifact.hashCode() : 0)
        return result
    }


    @Override
    public String toString() {
        return "GroupArtifact($group:$artifact)"
    }
}
