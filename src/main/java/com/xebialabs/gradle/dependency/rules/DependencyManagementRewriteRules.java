package com.xebialabs.gradle.dependency.rules;

import java.util.Map;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler;

import com.xebialabs.gradle.dependency.DependencyManagementContainer;
import com.xebialabs.gradle.dependency.domain.GroupArtifact;

public class DependencyManagementRewriteRules implements Action<ComponentModuleMetadataHandler> {

    private final DependencyManagementContainer container;
    private final Project project;

    public DependencyManagementRewriteRules(DependencyManagementContainer container, Project project) {
        this.container = container;
        this.project = project;
    }

    @Override
    public void execute(ComponentModuleMetadataHandler handler) {
        project.getLogger().debug("Configuring rewrites for project {}", project);
        Map<GroupArtifact, GroupArtifact> rewrites = container.getRewrites();

        for (Map.Entry<GroupArtifact, GroupArtifact> entry : rewrites.entrySet()) {
            GroupArtifact fromGa = entry.getKey();
            GroupArtifact toGa = entry.getValue();
            project.getLogger().debug("Replacing {} with {}", fromGa, toGa);

            final String moduleNotation = fromGa.getGroup() + ":" + fromGa.getArtifact();
            handler.module(moduleNotation, moduleMetadata ->
                    moduleMetadata.replacedBy(toGa.getGroup() + ":" + toGa.getArtifact(), "replaced by dependency management plugin"));
        }
    }
}
