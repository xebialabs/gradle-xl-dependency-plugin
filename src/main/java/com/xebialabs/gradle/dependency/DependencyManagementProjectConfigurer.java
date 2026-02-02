package com.xebialabs.gradle.dependency;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;

import com.xebialabs.gradle.dependency.domain.GroupArtifact;
import com.xebialabs.gradle.dependency.domain.GroupArtifactVersion;
import com.xebialabs.gradle.dependency.rules.DependencyManagementExclusionRules;
import com.xebialabs.gradle.dependency.rules.DependencyManagementRewriteRules;

public class DependencyManagementProjectConfigurer {

    public static void configureProject(Project project, DependencyManagementContainer container) {
        // Contract for all is that it executes the closure for all currently assigned objects, and any objects added later.
        project.getConfigurations().all(config -> {
            if (!container.isUseJavaPlatform()) {
                if (!"zinc".equals(config.getName())) { // The Scala compiler 'zinc' configuration should not be managed by us
                    config.resolutionStrategy(rs -> rs.eachDependency(manageDependency(project, container)));
                    configureExcludes(project, config, container);
                }
            }
        });
    }

    private static void configureExcludes(Project project, Configuration config, DependencyManagementContainer container) {
        for (GroupArtifact ga : container.getBlackList()) {
            if (!container.getRewrites().containsKey(ga)) {
                // exclude only dependencies that do NOT have a rewrite
                project.getLogger().debug("Excluding " + ga.toMap() + " from configuration " + config.getName());
                config.exclude(ga.toMap());
            }
        }
    }

    private static void rewrite(DependencyManagementContainer container, DependencyResolveDetails details) {
        var rewrites = container.getRewrites();

        GroupArtifact fromGa = new GroupArtifact(details.getRequested().getGroup(), details.getRequested().getName());
        GroupArtifact groupArtifact = rewrites.get(fromGa);
        if (groupArtifact != null) {
            String requestedVersion = container.getManagedVersion(details.getRequested().getGroup(), details.getRequested().getName());
            if (requestedVersion == null) {
                requestedVersion = details.getRequested().getVersion();
            }

            String rewriteVersion = container.getManagedVersion(groupArtifact.getGroup(), groupArtifact.getArtifact());
            GroupArtifactVersion groupArtifactVersion;
            if (rewriteVersion != null) {
                groupArtifactVersion = groupArtifact.withVersion(rewriteVersion);
            } else {
                groupArtifactVersion = groupArtifact.withVersion(requestedVersion);
            }

            details.useTarget(groupArtifactVersion.toMap(details.getRequested()));
            details.because("replaced by dependency management plugin rewrite");
        }
    }

    private static Action<? super DependencyResolveDetails> manageDependency(Project project, DependencyManagementContainer container) {
        return details -> {
            container.resolveIfNecessary();
            rewrite(container, details);
            enforceVersion(project, container, details);
        };
    }

    private static void enforceVersion(Project project, DependencyManagementContainer container, DependencyResolveDetails details) {
        String version = container.getManagedVersion(details.getRequested().getGroup(), details.getRequested().getName());
        if (version != null) {
            if (version.startsWith("${")) {
                // version is not resolved
                project.getLogger().info("Unresolved version {} for {}:{}. Will not use it as a version.",
                        version,
                        details.getRequested().getGroup(),
                        details.getRequested().getName());
            } else {
                project.getLogger().debug("Resolved version {} for {}:{}",
                        version,
                        details.getRequested().getGroup(),
                        details.getRequested().getName());
                details.useVersion(version);
            }
        } else {
            project.getLogger().debug("No managed version for {}:{} --> using version {}",
                    details.getRequested().getGroup(),
                    details.getRequested().getName(),
                    details.getRequested().getVersion());
        }
    }

    public static void configureRewrites(DependencyManagementContainer container, Project project) {
        // always rewrite dependencies - even when using legacy plugin implementation
        project.getDependencies().modules(new DependencyManagementRewriteRules(container, project));
        // While gradle works with rewrite above, IntelliJ IDEA does not. So we need to rewrite dependencies in configurations as well.
        project.getConfigurations().configureEach(conf ->
                conf.resolutionStrategy(rs ->
                        rs.eachDependency(details ->
                                rewrite(container, details))
                )
        );
    }

    public static void configureExcludeRules(DependencyManagementContainer container, Project project) {
        if (container.isUseJavaPlatform()) {
            project.getDependencies().components(new DependencyManagementExclusionRules(container, project));
        }
    }
}
