package com.xebialabs.gradle.dependency.supplier

import com.xebialabs.gradle.dependency.DependencyManagementContainer

interface DependencyManagementSupplier {

  def collectDependencies(DependencyManagementContainer container)

  def collectVersions(DependencyManagementContainer container)

  def collectExclusions(DependencyManagementContainer container)

  def collectRewrites(DependencyManagementContainer container)
}
