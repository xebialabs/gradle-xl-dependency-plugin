package com.xebialabs.gradle.plugins.dependency

import com.xebialabs.gradle.plugins.dependency.domain.GroupArtifact
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
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

        blackListDependencies(project, container)

        project.allprojects.each { Project p ->
            //noinspection GroovyAssignabilityCheck
            p.configurations.all { Configuration config ->
                config.resolutionStrategy { ResolutionStrategy rs ->
                    rs.eachDependency(forceVersion(container))
                }
            }

        }
    }

    private void blackListDependencies(Project project, DependencyManagementContainer container) {
        def blackList = container.blackList.collect { GroupArtifact ga -> [group: ga.group, artifact: ga.artifact] }

        project.allprojects.each { Project p ->
            p.configurations.all { Configuration config ->
                DomainObjectSet<ModuleDependency> moduleDependencies = config.dependencies.withType(ModuleDependency)
                moduleDependencies.each { ModuleDependency dependency ->
                    blackList.each { b ->
                        dependency.exclude(b)
                    }
                }
            }
        }

//        project.allprojects*.configurations.all*.dependencies*.withType(ModuleDependency)*.each { ModuleDependency dependency ->
//            blackList.each { b ->
//                dependency.exclude(b)
//            }
//        }
    }

    Action<DependencyResolveDetails> forceVersion(DependencyManagementContainer container) {
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
