package com.xebialabs.gradle.dependency.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import com.typesafe.config.*;

import com.xebialabs.gradle.dependency.DependencyManagementContainer;

public class ExportConfTask extends DefaultTask {

    private static final String EMPTY_CONFIG_TEMPLATE =
            """
                    dependencyManagement {
                        versions {}
                        dependencies: []
                        blacklist: []
                        rewrites {}
                    }
                    """;

    private final RegularFileProperty outputFile;
    private final ConfigurableFileCollection inputFiles;
    private DependencyManagementContainer dependencyManagementContainer;
    private final ProjectLayout projectLayout;

    @Inject
    public ExportConfTask(ObjectFactory objectFactory, ProjectLayout projectLayout) {
        this.projectLayout = projectLayout;
        this.outputFile = objectFactory.fileProperty();
        this.inputFiles = objectFactory.fileCollection();
    }

    @Internal
    public DependencyManagementContainer getDependencyManagementContainer() {
        return dependencyManagementContainer;
    }

    public void setDependencyManagementContainer(DependencyManagementContainer dependencyManagementContainer) {
        this.dependencyManagementContainer = dependencyManagementContainer;
    }

    @OutputFile
    public RegularFileProperty getOutputFile() {
        return outputFile;
    }

    @InputFiles
    public ConfigurableFileCollection getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(Collection<File> suppliedConfigFiles) {
        inputFiles.setFrom(suppliedConfigFiles);
    }

    protected Config mergeConfigList(String path, Config c1, Config c2) {
        List<Object> v1 = new ArrayList<>(c1.getList(path).unwrapped());
        List<Object> v2 = new ArrayList<>();
        if (c2.hasPath(path)) {
            v2 = c2.getList(path).unwrapped();
        }
        v1.addAll(v2);
        var newValue = ConfigValueFactory.fromIterable(v1);
        return c1.withValue(path, newValue);
    }

    protected Config mergeConfig(String path, Config c1, Config c2) {
        Config v1 = c1.getConfig(path);
        if (c2.hasPath(path)) {
            Config v2 = c2.getConfig(path);
            Config newValue = v1.withFallback(v2);
            return c1.withValue(path, newValue.root());
        } else {
            return c1;
        }
    }

    protected Config mergeConfigs(Config c1, Config c2) {
        Config merged = mergeConfigList("dependencyManagement.dependencies", c1, c2);
        merged = mergeConfigList("dependencyManagement.blacklist", merged, c2);
        merged = mergeConfig("dependencyManagement.rewrites", merged, c2);
        merged = mergeConfig("dependencyManagement.versions", merged, c2);
        return merged;
    }

    @TaskAction
    public void exportConf() throws IOException {
        Config emptyConfig = ConfigFactory.parseString(EMPTY_CONFIG_TEMPLATE);
        List<Config> configs = dependencyManagementContainer.getConfigs();

        Config mergedConfig = configs.stream()
                .filter(config -> !config.isEmpty())
                .reduce(emptyConfig, this::mergeConfigs);

        RegularFile outputConf = outputFile.get();
        File outputConfFile = outputConf.getAsFile();
        outputConfFile.createNewFile();

        getLogger().info("Updating content of " + outputConfFile.getPath());

        ConfigResolveOptions resolveOptions = ConfigResolveOptions.defaults().setAllowUnresolved(true);
        Config resolvedConfig = mergedConfig.resolve(resolveOptions);
        ConfigRenderOptions renderOptions = ConfigRenderOptions.defaults()
                .setFormatted(true)
                .setComments(true)
                .setJson(false)
                .setOriginComments(false);

        String renderedConfig = resolvedConfig.root().render(renderOptions);
        List<String> renderedConfigs = List.of(renderedConfig);

        Files.write(outputConfFile.toPath(), renderedConfigs);
    }
}
