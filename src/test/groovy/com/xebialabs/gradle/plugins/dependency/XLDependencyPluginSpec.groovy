package com.xebialabs.gradle.plugins.dependency

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class XLDependencyPluginSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    Project project
    File artifactDir

    def setup() {
        def projectDir = temporaryFolder.newFolder("test-project")
        def folder = new File(projectDir, "gradle")
        folder.mkdir()
        writeFile(new File(folder, "dependencies.conf"), "dependencyManagement { versions { junitVersion: \"4.12\" } }")
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        def repoDir = temporaryFolder.newFolder("repo")

        artifactDir = new File(repoDir, "test/dependencies/1.0")
        artifactDir.mkdirs()

        project.repositories {
            mavenCentral()
            maven {
                url "file://${repoDir}"
            }
        }
    }

    def "should apply the version override file from the rootProject"() {
        when:
        project.apply plugin: "xebialabs.dependency"

        then:
        project.extensions.getByType(ExtraPropertiesExtension).has("junitVersion")
        project.extensions.getByType(ExtraPropertiesExtension).get("junitVersion") == "4.12"
    }

    def "should apply a versions file from the currently built project"() {
        given:
        writeFile(project.file("extra-versions.conf"), "dependencyManagement { versions { overthereVersion: \"4.2.0\" } }")
        project.apply plugin: "xebialabs.dependency"

        when:
        project.dependencyManagement {
                importConf project.file("extra-versions.conf")
        }

        then:
        project.extensions.getByType(ExtraPropertiesExtension).has("overthereVersion")
        project.extensions.getByType(ExtraPropertiesExtension).get("overthereVersion") == "4.2.0"
    }

    def "should apply a dependencies file with a dependency local to the project"() {
        given:
        writeFile(project.file("dependencies.conf"), """
dependencyManagement.dependencies: [
    "junit:junit:\$junitVersion"
]""")

        project.apply plugin: "xebialabs.dependency"
        project.apply plugin: "java"
        project.dependencyManagement {
            importConf project.file("dependencies.conf")
        }
        project.dependencies {
            compile "junit:junit"
        }

        when:
        def files = project.configurations.compile.resolve()

        then:
        files.size() >= 1
        files.collect { it.name }.contains('junit-4.12.jar')
    }

    def "should apply a dependencies file with a dependency-set local to the project"() {
        given:
        writeFile(project.file("dependencies.conf"), """
dependencyManagement.dependencies: [
    {
        group: "ch.qos.logback"
        version: "1.1.3"
        artifacts: [ "logback-classic", "logback-core" ]
    }
]""")

        project.apply plugin: "xebialabs.dependency"
        project.apply plugin: "java"
        project.dependencyManagement {
            importConf project.file("dependencies.conf")
        }
        project.dependencies {
            compile "ch.qos.logback:logback-core"
        }

        when:
        def files = project.configurations.compile.resolve()

        then:
        files.size() >= 1
        files.collect { it.name }.contains('logback-core-1.1.3.jar')
    }

    def "should resolve dependencies artifact"() {
        given:
        writeFile(new File(artifactDir, "dependencies-1.0-depmgmt.conf"), """
dependencyManagement.dependencies: [
    "junit:junit:\$junitVersion"
]""")
        project.apply plugin: "xebialabs.dependency"
        project.apply plugin: "java"
        project.dependencyManagement {
            importConf "test:dependencies:1.0:depmgmt"
        }

        project.dependencies {
            compile "junit:junit"
        }

        when:
        def files = project.configurations.compile.resolve()

        then:
        files.size() >= 1
        files.collect { it.name }.contains('junit-4.12.jar')
    }

    def "should rewrite dependencies"() {
        given:
        writeFile(project.file("dependencies.conf"), """
dependencyManagement.rewrites {
    "foo:bar": "ch.qos.logback:logback-core"
}""")

        project.apply plugin: "xebialabs.dependency"
        project.apply plugin: "java"

        project.dependencyManagement {
            importConf project.file("dependencies.conf")
        }

        project.dependencies {
            compile "foo:bar:1.1.3"
        }

        when:
        def files = project.configurations.compile.resolve()

        then:
        files.size() >= 1
        files.collect { it.name }.contains('logback-core-1.1.3.jar')
    }

    def "should exclude dependencies"() {
        given:
        writeFile(project.file("dependencies.conf"), """
dependencyManagement.dependencies: [
    "junit:junit:\$junitVersion"
]
dependencyManagement.blacklist: [
    "org.hamcrest"
]""")

        project.apply plugin: "xebialabs.dependency"
        project.apply plugin: "java"

        project.dependencyManagement {
            importConf project.file("dependencies.conf")
        }

        project.dependencies {
            compile "junit:junit"
        }

        when:
        def files = project.configurations.compile.resolve()

        then:
        files.size() == 1
        files.collect { it.name }.contains('junit-4.12.jar')
        !files.collect { it.name }.contains('hamcrest-core-1.1.3.jar')
    }

    private def writeFile(File file, String content) {
        file.withWriter { BufferedWriter writer ->
            writer.write(content)
        }
    }
}
