package com.xebialabs.gradle.plugins.dependency.domain

class GroupArtifact {

    String group

    String artifact

    GroupArtifact(String group, String artifact) {
        this.group = group
        this.artifact = artifact
    }
}
