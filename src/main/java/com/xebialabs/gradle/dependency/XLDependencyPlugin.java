package com.xebialabs.gradle.dependency;

import java.io.File;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.xebialabs.gradle.dependency.supplier.ProjectSupplier;

public class XLDependencyPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("com.xebialabs.dependency.base");

        File defaultConfFile = project.file("gradle/dependencies.conf");

        DependencyManagementExtension ext = project.getExtensions().getByType(DependencyManagementExtension.class);

        ext.supplier(new ProjectSupplier(project));
        if (defaultConfFile.exists()) {
            try {
                ext.importConf(defaultConfFile);
            } catch (Exception e) {
                throw new RuntimeException("Failed to import dependency configuration from " + defaultConfFile, e);
            }
        }
    }
}
