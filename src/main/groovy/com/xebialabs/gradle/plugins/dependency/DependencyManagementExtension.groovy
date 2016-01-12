package com.xebialabs.gradle.plugins.dependency

import com.xebialabs.gradle.plugins.dependency.supplier.DependencySupplier
import com.xebialabs.gradle.plugins.dependency.supplier.FileSupplier
import org.gradle.api.GradleException
import org.gradle.api.Project

class DependencyManagementExtension {

    DependencyManagementContainer container
    Project project

    DependencyManagementExtension(Project project, DependencyManagementContainer container) {
        this.project = project
        this.container = container
    }

    def importConf(File f) {
        if (f.exists()) {
            project.logger.info("Added dependency management file: $f")
            container.addSupplier(new FileSupplier(f))
        } else {
            throw new GradleException("Could not locate $f")
        }
    }

    def importConf(String s) {
        project.logger.info("Added dependency management artifact: $s")
        container.addSupplier(new DependencySupplier(this.project, s))
    }
}
