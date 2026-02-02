package com.xebialabs.gradle.dependency.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ComponentMetadataDetails;

import com.xebialabs.gradle.dependency.DependencyManagementContainer;
import com.xebialabs.gradle.dependency.domain.GroupArtifact;

public class DependencyManagementExclusionRule implements Action<ComponentMetadataDetails> {

    private final Set<String> forbiddenDependencies = new HashSet<>();
    private final DependencyManagementContainer container;

    public DependencyManagementExclusionRule(DependencyManagementContainer container) {
        this.container = container;
        for (GroupArtifact ga : container.getBlackList()) {
            this.forbiddenDependencies.add(ga.getGroup() + ":" + ga.getArtifact());
        }
    }

    @Override
    public void execute(ComponentMetadataDetails componentMetadataDetails) {
        String moduleName = componentMetadataDetails.getId().getGroup() + ":" + componentMetadataDetails.getId().getName();
        List<String> moduleExcludes = container.getManagedExcludes().getOrDefault(moduleName, List.of());
        Set<String> moduleExcludesSet = new HashSet<>(moduleExcludes);

        componentMetadataDetails.allVariants(variant -> variant.withDependencies(dependencies -> {
            dependencies.removeIf(d -> {
                String dependencyKey = d.getGroup() + ":" + d.getName();
                boolean shouldBeExcluded = forbiddenDependencies.contains(dependencyKey) || moduleExcludesSet.contains(dependencyKey);
                boolean isNotRewritten = !container.getRewrites().containsKey(new GroupArtifact(d.getGroup(), d.getName()));
                return shouldBeExcluded && isNotRewritten;
            });
        }));
    }
}
