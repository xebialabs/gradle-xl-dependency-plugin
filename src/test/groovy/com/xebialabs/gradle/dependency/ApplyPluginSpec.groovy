package com.xebialabs.gradle.dependency

import nebula.test.ProjectSpec
import org.gradle.api.GradleException
import org.gradle.api.internal.plugins.PluginApplicationException

class ApplyPluginSpec extends ProjectSpec {
  def pluginName = 'com.xebialabs.dependency'

  def setup() {
    def folder = new File(projectDir, "gradle")
    folder.mkdir()
    new File(folder, 'dependencies.conf') << '''
      dependencyManagement {
        versions {
          junitVersion: "4.12"
        }
      }
    '''
  }

  def 'apply does not throw exceptions'() {
    when:
    project.apply plugin: pluginName

    then:
    noExceptionThrown()
  }

  def 'apply is idempotent'() {
    when:
    project.apply plugin: pluginName
    project.apply plugin: pluginName

    then:
    noExceptionThrown()
  }

  def 'should fail to apply on a sub project'() {
    def sub = addSubproject('sub')
    project.subprojects.add(sub)

    when:
    sub.apply plugin: pluginName

    then:
    thrown PluginApplicationException
  }

  def "should fail to apply when 'gradle/dependencies.conf' is missing"() {
    given:
    new File(projectDir, 'gradle/dependencies.conf').delete()

    when:
    project.apply plugin: pluginName

    then:
    def ex = thrown(GradleException)
    ex.getClass() == PluginApplicationException
    ex.getCause().getClass() == FileNotFoundException
    ex.getCause().getMessage() =~ "Cannot configure dependency management from non-existing file .*/gradle/dependencies.conf"
  }
}
