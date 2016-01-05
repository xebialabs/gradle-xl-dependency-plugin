package com.xebialabs.gradle.plugins.dependency

import com.xebialabs.gradle.plugins.dependency.supplier.DependencySupplier
import com.xebialabs.gradle.plugins.dependency.supplier.FileSupplier
import org.gradle.api.Project

class DependencyManagementExtension {

    DependencyManagementContainer container
    Project project

    DependencyManagementExtension(Project project, DependencyManagementContainer container) {
        this.project = project
        this.container = container
    }

    def importConf(File f) {
        container.addSupplier(new FileSupplier(f))
    }

    def importConf(String s) {
        container.addSupplier(new DependencySupplier(this.project, s))
    }
}
