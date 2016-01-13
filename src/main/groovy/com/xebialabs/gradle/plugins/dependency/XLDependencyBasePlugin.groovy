package com.xebialabs.gradle.plugins.dependency

import com.xebialabs.gradle.plugins.dependency.domain.GroupArtifact
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolutionStrategy

class XLDependencyBasePlugin implements Plugin<Project> {
    Project project

    public void apply(Project project) {
        assert project.rootProject == project: "Please apply plugin 'xebialabs.dependency.base' on the rootProject only!"
        this.project = project
        DependencyManagementContainer container = new DependencyManagementContainer(project)
        project.getExtensions().create("dependencyManagement", DependencyManagementExtension.class, project, container);

        project.allprojects.each { Project p ->
            //noinspection GroovyAssignabilityCheck
            p.configurations.all { Configuration config ->
                config.resolutionStrategy { ResolutionStrategy rs ->
                    rs.eachDependency(rewrite(container))
                    rs.eachDependency(forceVersion(container))
                }
            }

        }
    }

    private Action<? super DependencyResolveDetails> rewrite(DependencyManagementContainer container) {
        return new Action<DependencyResolveDetails>() {
            @Override
            void execute(DependencyResolveDetails details) {
                container.resolveIfNecessary()
                def rewrites = container.rewrites
                GroupArtifact groupArtifact = rewrites[new GroupArtifact(details.requested.group, details.requested.name)]
                if (groupArtifact) {
                    details.useTarget(groupArtifact.toMap(details.requested))
                }
            }
        }
    }

    private Action<DependencyResolveDetails> forceVersion(DependencyManagementContainer container) {
        return new Action<DependencyResolveDetails>() {
            @Override
            void execute(DependencyResolveDetails details) {
                container.resolveIfNecessary()
                def version = container.getManagedVersion(details.requested.group, details.requested.name)
                if (version) {
                    project.logger.info("Resolved version $version for ${details.requested.group}:${details.requested.name}")
                    details.useVersion(version)
                } else {
                    project.logger.info("No managed version for ${details.requested.group}:${details.requested.name} --> using version ${details.requested.version}")
                }
            }
        }
    }
}
