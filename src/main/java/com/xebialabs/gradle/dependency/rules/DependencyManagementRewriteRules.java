package com.xebialabs.gradle.dependency.rules;

import com.xebialabs.gradle.dependency.DependencyManagementContainer;
import com.xebialabs.gradle.dependency.domain.GroupArtifact;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler;

import java.util.Map;

public class DependencyManagementRewriteRules implements Action<ComponentModuleMetadataHandler> {
  
  private final DependencyManagementContainer container;
  private final Project project;

  public DependencyManagementRewriteRules(DependencyManagementContainer container, Project project) {
    this.container = container;
    this.project = project;
  }

  @Override
  public void execute(ComponentModuleMetadataHandler handler) {
    project.getLogger().debug("Configuring rewrites for project " + project);
    Map<GroupArtifact, GroupArtifact> rewrites = container.getRewrites();
    
    for (Map.Entry<GroupArtifact, GroupArtifact> entry : rewrites.entrySet()) {
      GroupArtifact fromGa = entry.getKey();
      GroupArtifact toGa = entry.getValue();
      project.getLogger().debug("Replacing " + fromGa + " with " + toGa);
      
      handler.module(fromGa.getGroup() + ":" + fromGa.getArtifact(), moduleMetadata -> {
        moduleMetadata.replacedBy(toGa.getGroup() + ":" + toGa.getArtifact(), "replaced by dependency management plugin");
      });
    }
  }
}
