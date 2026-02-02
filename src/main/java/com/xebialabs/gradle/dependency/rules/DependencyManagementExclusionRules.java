package com.xebialabs.gradle.dependency.rules;

import java.util.Map;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;

import com.xebialabs.gradle.dependency.DependencyManagementContainer;

public class DependencyManagementExclusionRules implements Action<ComponentMetadataHandler> {

    private final Project project;
    private final DependencyManagementContainer container;

    public DependencyManagementExclusionRules(DependencyManagementContainer container, Project project) {
        this.project = project;
        this.container = container;
    }

    @Override
    public void execute(ComponentMetadataHandler componentMetadataHandler) {
        DependencyManagementExclusionRule exclusionRule = new DependencyManagementExclusionRule(container);
        componentMetadataHandler.all(exclusionRule);
        // per component excludes:
        for (Map.Entry<String, ?> entry : container.getManagedExcludes().entrySet()) {
            String module = entry.getKey();
            componentMetadataHandler.withModule(module, exclusionRule);
        }
    }
}
