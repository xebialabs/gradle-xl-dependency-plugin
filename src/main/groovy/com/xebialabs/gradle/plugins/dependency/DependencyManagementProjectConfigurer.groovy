package com.xebialabs.gradle.plugins.dependency

import com.xebialabs.gradle.plugins.dependency.domain.GroupArtifact
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.plugins.MavenPlugin

class DependencyManagementProjectConfigurer {

    static def configureProject(Project project, DependencyManagementContainer container) {
        project.configurations.all { Configuration config ->
            config.resolutionStrategy { ResolutionStrategy rs ->
                rs.eachDependency(rewrite(project, container))
                rs.eachDependency(forceVersion(project, container))
            }
        }

        project.afterEvaluate {
            project.plugins.withType(MavenPlugin) { plugin ->
                def installer = project.tasks.findByName("install")?.repositories?.mavenInstaller
                def deployer = project.tasks.getByName("uploadArchives").repositories.mavenDeployer

                [installer, deployer].findAll { it != null }*.pom*.whenConfigured { pom ->
                    def dependencyMap = [:]
                    if (project.configurations.findByName("runtime")) {
                        dependencyMap['runtime'] = project.configurations.runtime.incoming.resolutionResult.allDependencies
                    }
                    if (project.configurations.findByName("testRuntime")) {
                        dependencyMap['test'] = project.configurations.testRuntime.incoming.resolutionResult.allDependencies - dependencyMap['runtime']
                    }
                    pom.dependencies.each { dep ->
                        def group = dep.groupId
                        def name = dep.artifactId
                        def scope = dep.scope

                        if (['provided', 'compile'].contains(scope)) {
                            scope = 'runtime'
                        }

                        ResolvedDependencyResult resolved = dependencyMap[scope].find { r ->
                            (r.requested instanceof ModuleComponentSelector) &&
                                    (r.requested.group == group) &&
                                    (r.requested.module == name)
                        }

                        if (!resolved) {
                            return  // continue loop if a dependency is not found in dependencyMap
                        }

                        dep.version = resolved?.selected?.moduleVersion?.version
                    }
                }
            }
        }
    }

    private
    static Action<? super DependencyResolveDetails> rewrite(Project project, DependencyManagementContainer container) {
        return new Action<DependencyResolveDetails>() {
            @Override
            void execute(DependencyResolveDetails details) {
                container.resolveIfNecessary()
                def rewrites = container.rewrites

                def fromGa = new GroupArtifact(details.requested.group, details.requested.name)
                GroupArtifact groupArtifact = rewrites[fromGa]
                if (groupArtifact) {
                    project.logger.info("Rewriting $fromGa -> $groupArtifact")
                    details.useTarget(groupArtifact.toMap(details.requested))
                }
            }
        }
    }

    private
    static Action<DependencyResolveDetails> forceVersion(Project project, DependencyManagementContainer container) {
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
