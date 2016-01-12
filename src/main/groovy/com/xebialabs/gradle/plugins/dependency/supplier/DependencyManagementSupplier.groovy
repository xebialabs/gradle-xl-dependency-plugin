package com.xebialabs.gradle.plugins.dependency.supplier

import com.xebialabs.gradle.plugins.dependency.DependencyManagementContainer

interface DependencyManagementSupplier {

    def collectDependencies(DependencyManagementContainer container)
    def collectVersions(DependencyManagementContainer container)
    def collectExclusions(DependencyManagementContainer container)
}