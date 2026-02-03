package com.xebialabs.gradle.dependency.supplier;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.attributes.LibraryElements;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class DependencySupplier implements ConfigSupplier {

    private final String dependency;
    private Config config;
    private final Project project;
    private final String extension;
    private final String classifier;
    private final String suffix;
    private static int configCounter = 0;

    public DependencySupplier(Project project, String dependency, String extension, String classifier) {
        this.project = project;
        this.dependency = dependency;
        this.extension = extension;
        this.classifier = classifier;
        this.suffix = createSuffix(classifier, extension);
    }

    private String createSuffix(String classifier, String extension) {
        String result = "";
        if (classifier != null && !classifier.trim().isEmpty()) {
            result = ":" + classifier;
        }
        if (extension != null && !extension.trim().isEmpty()) {
            result = result + "@" + extension;
        }
        return result;
    }

    @Override
    public Config getConfig(ConfigFileCollector collector) {
        if (config == null) {
            Dependency dep = project.getDependencies().create(dependency + suffix);
            
            // Use a named resolvable configuration instead of detached configuration
            // This ensures Gradle can properly wire task dependencies in composite builds
            String confName = "xlDependencyManagementConf" + (configCounter++);
            Configuration xlRefConf = project.getConfigurations().create(confName, conf -> {
                conf.setVisible(false);
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(true);
                conf.setTransitive(false);
                conf.attributes(attributes ->
                        attributes.attribute(
                                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                project.getObjects().named(LibraryElements.class, "conf-file")
                        )
                );
            });
            
            xlRefConf.getDependencies().add(dep);

            // Log resolution for debugging composite builds
            project.getLogger().info("Resolving dependency management from: {}", dependency);

            File resolvedFile;
            try {
                // For composite builds: Build the artifact if it doesn't exist yet
                // This handles the case where resolution happens during configuration phase
                buildIncludedArtifactIfNeeded(xlRefConf);
                
                // In composite builds, this will use the locally built artifact
                resolvedFile = xlRefConf.getSingleFile();
                project.getLogger().info("Resolved dependency management to file: {}", resolvedFile.getAbsolutePath());
            } catch (Exception e) {
                project.getLogger().error("Failed to resolve dependency: {}. " +
                    "If using composite builds, ensure the included project has " +
                    "the 'com.xebialabs.dependency.platform' plugin applied and " +
                    "the correct group configured.", dependency);
                throw e;
            } finally {
                // Clean up the configuration after resolution
                project.getConfigurations().remove(xlRefConf);
            }

            collector.collect(resolvedFile);

            if (!resolvedFile.exists()) {
                throw new AssertionError("Dependency " + dependency +
                    " was resolved but file does not exist: " + resolvedFile.getAbsolutePath());
            }

            config = ConfigFactory.parseFile(resolvedFile).resolve();
        }
        return config;
    }
    
    /**
     * For composite builds: Ensure the artifact is built before we try to access it.
     * This works around Gradle's limitation that configuration-time resolution
     * doesn't automatically build included build artifacts.
     */
    private void buildIncludedArtifactIfNeeded(Configuration configuration) {
        try {
            // Try to get the artifact path without fully resolving
            var artifacts = configuration.getIncoming().getArtifacts();
            for (var artifact : artifacts) {
                var file = artifact.getFile();
                
                // If file doesn't exist and this is from an included build, build it
                if (!file.exists()) {
                    project.getLogger().warn("Artifact {} does not exist yet. " +
                        "Attempting to build it from included build...", file.getAbsolutePath());
                    
                    // Try to extract project/task information and build it
                    // This is a heuristic - if the path contains "/build/", it's likely a task output
                    String path = file.getAbsolutePath();
                    if (path.contains("/build/")) {
                        // Extract the likely project path and task name
                        // For example: /path/to/xl-platform/xl-reference/build/xl-reference.conf
                        // We need to run: xl-platform:xl-reference:exportDependencyManagementConf
                        
                        // This is a best-effort approach for composite builds
                        buildArtifactViaGradle(file);
                    }
                }
            }
        } catch (Exception e) {
            // If this fails, we'll let the normal resolution proceed and fail there if needed
            project.getLogger().debug("Could not pre-build included artifact: {}", e.getMessage());
        }
    }
    
    /**
     * Attempt to build the artifact by invoking Gradle on the included build.
     * This is necessary because Gradle doesn't automatically build artifacts
     * when they're resolved during configuration phase in composite builds.
     */
    private void buildArtifactViaGradle(File artifactFile) {
        try {
            String absolutePath = artifactFile.getAbsolutePath();
            
            // Try to find the included build's root directory
            // Assumption: artifact is in project-root/subproject/build/artifact.conf
            String buildDir = absolutePath.substring(0, absolutePath.lastIndexOf("/build/"));
            
            // Check if this looks like an included build project
            File projectDir = new File(buildDir);
            File rootDir = projectDir.getParentFile();
            
            if (projectDir.exists() && new File(projectDir, "build.gradle").exists() && rootDir != null) {
                project.getLogger().warn("Executing export task in included build at: {}", projectDir);
                
                // Build the gradle command
                File gradlew = new File(rootDir, "gradlew");
                if (!gradlew.exists()) {
                    gradlew = new File(rootDir, "gradlew.bat");
                }
                
                if (gradlew.exists()) {
                    String taskName = ":" + projectDir.getName() + ":exportDependencyManagementConf";
                    
                    ProcessBuilder pb = new ProcessBuilder(
                        gradlew.getAbsolutePath(),
                        taskName,
                        "--quiet"
                    );
                    pb.directory(rootDir);
                    pb.inheritIO(); // This will show output in the console
                    
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    
                    if (exitCode == 0) {
                        project.getLogger().info("Successfully built artifact in included build");
                    } else {
                        project.getLogger().warn("Failed to build included artifact (exit code: {})", exitCode);
                    }
                }
            }
        } catch (Exception e) {
            project.getLogger().debug("Failed to build included artifact: {}", e.getMessage());
            // Continue - let normal resolution handle the error if file still doesn't exist
        }
    }
}
