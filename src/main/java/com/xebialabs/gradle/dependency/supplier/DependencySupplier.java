package com.xebialabs.gradle.dependency.supplier;

import java.io.File;
import org.gradle.api.Project;
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
            var xlRefConf = project.getConfigurations().detachedConfiguration(dep);
            xlRefConf.attributes(attributes ->
                    attributes.attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            project.getObjects().named(LibraryElements.class, "conf-file")
                    )
            );
            xlRefConf.setTransitive(false);
            File resolvedFile = xlRefConf.getSingleFile();
            collector.collect(resolvedFile);

            if (!resolvedFile.exists()) {
                throw new AssertionError("Dependency " + dependency + " was not resolved into a file");
            }

            config = ConfigFactory.parseFile(resolvedFile).resolve();
        }
        return config;
    }
}
