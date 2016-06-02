package com.xebialabs.gradle.dependency

import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator

class XLDependencyPluginSpec extends IntegrationSpec {
  File repoDir

  def setup() {
    // create the project local gradle/dependencies.conf
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        versions {
          junitVersion: "4.12"
        }
      }
    '''

    // TODO: the repo is never cleaned
    def graph = new DependencyGraph(['commons-lang:commons-lang:2.5',
                                     'commons-lang:commons-lang:2.6',
                                     'ch.qos.logback:logback-core:1.1.3',
                                     'org.hamcrest:hamcrest-core:1.1.2',
                                     'org.hamcrest:hamcrest-core:1.1.3',
                                     'com.netflix.nebula:nebula-test:3.1.0 -> commons-lang:commons-lang:2.6',
                                     'junit:junit:4.11 -> org.hamcrest:hamcrest-core:1.1.2',
                                     'junit:junit:4.12 -> org.hamcrest:hamcrest-core:1.1.3'])
    def generator = new GradleDependencyGenerator(graph)
    repoDir = generator.generateTestMavenRepo()

    // sadly the generator does not create zips
    createFile('zip-dependency-1.0.zip', directory('test/zip-dependency/1.0', repoDir))

//    repoDir.eachFileRecurse {
//      println it
//    }
  }

  def configureRepositories(project) {
    project.repositories {
      maven {
        url "file://${repoDir.getAbsolutePath()}"
      }
    }
  }

  def "should apply the version override file from the rootProject"() {
    given:
    baseBuildFile(['xebialabs.dependency']) << """
    assert junitVersion == '4.12'
    """
    when:
    runTasksSuccessfully('build')

    then:
    noExceptionThrown()
  }

  def "should apply the version override file from the rootProject in a sub project"() {
    given:
    settingsFile << "include 'sub'"
    buildFile << """
    apply plugin: 'xebialabs.dependency'

    assert allprojects.size() == 2
    allprojects {
      repositories {
        maven {
          url "file://${repoDir.getAbsolutePath()}"
        }
      }
      assert junitVersion == '4.12'
    }
    """
    when:
    runTasksSuccessfully('build')

    then:
    noExceptionThrown()
  }

  def "should apply a reference file from the currently built project"() {
    given:
    baseBuildFile(['xebialabs.dependency']) << """
      dependencyManagement {
        importConf project.file("reference.conf")
      }
      assert junitVersion == '4.12'
      assert overthereVersion == '4.2.0'
    """
    createFile("reference.conf") << '''
      dependencyManagement {
        versions {
          overthereVersion: "4.2.0"
        }
      }
    '''
    when:
    runTasksSuccessfully('build')

    then:
    noExceptionThrown()
  }

  def "should take version from gradle/dependencies.conf over reference.conf"() {
    baseBuildFile(['xebialabs.dependency']) << """
      dependencyManagement {
        importConf project.file("reference.conf")
      }
      assert junitVersion == '4.12'
    """
    createFile("reference.conf") << '''
      dependencyManagement {
        versions {
          junitVersion = "4.0"
        }
      }
    '''
    when:
    runTasksSuccessfully('build')

    then:
    noExceptionThrown()
  }

  def "should substitute placeholders"() {
    baseBuildFile(['xebialabs.dependency']) << """
      dependencyManagement {
        importConf project.file("reference.conf")
      }

      assert testVersion == '4.0'
    """
    createFile("reference.conf") << '''
      someProperty: "4.0"
      dependencyManagement {
        versions {
          testVersion = ${someProperty}
        }
      }
    '''
    when:
    runTasksSuccessfully('build')
    given:

    then:
    noExceptionThrown()
  }

  def "should apply a dependencies file with a dependency local to the project"() {
    baseBuildFile(['xebialabs.dependency', 'java']) << writeDepTask() << """
      dependencyManagement {
        importConf project.file("dependencies.conf")
      }

      dependencies {
        compile 'commons-lang:commons-lang'
      }
    """
    createFile("dependencies.conf") << '''
      dependencyManagement {
        dependencies: [ "commons-lang:commons-lang:2.6" ]
      }
    '''
    when:
    runTasksSuccessfully('writeDeps')

    then:
    noExceptionThrown()
    def files = new File(projectDir, 'artifacts').listFiles()
    files.size() >= 1
    files.collect { it.name }.contains('commons-lang-2.6.jar')
  }

  def "should apply a dependencies file with a dependency-set local to the project"() {
    baseBuildFile(['xebialabs.dependency', 'java']) << writeDepTask() << """
      dependencyManagement {
        importConf project.file("dependencies.conf")
      }

      dependencies {
        compile 'ch.qos.logback:logback-core'
      }
    """
    createFile("dependencies.conf") << '''
      dependencyManagement.dependencies: [
        {
          group: "ch.qos.logback"
          version: "1.1.3"
          artifacts: [ "logback-classic", "logback-core" ]
        }
      ]
    '''

    when:
    runTasksSuccessfully('writeDeps')

    then:
    noExceptionThrown()
    def fileNames = new File(projectDir, 'artifacts').listFiles().collect({ it.name }) as Set
    fileNames.size() >= 1
    fileNames.contains('logback-core-1.1.3.jar')
  }

  def "should override versions with project properties"() {
    given:
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        versions {
          junitVersion: "4.12"
        }
        dependencies: [ "junit:junit:$junitVersion" ]
      }
    '''
    baseBuildFile(['xebialabs.dependency', 'java']) << writeDepTask() << """
      dependencyManagement {
        importConf project.file("reference.conf")
      }

      dependencies {
        compile 'commons-lang:commons-lang'
        compile 'junit:junit'
      }
    """
    createFile("reference.conf") << '''
      dependencyManagement {
        versions {
          commonsLangVersion: "2.6"
        }
        dependencies: [ "commons-lang:commons-lang:$commonsLangVersion" ]
      }
    '''
    when:
    runTasksSuccessfully('writeDeps', '-PdependencyManagement.versions.junitVersion=4.11', '-PdependencyManagement.versions.commonsLangVersion=2.5')

    then:
    noExceptionThrown()
    def fileNames = new File(projectDir, 'artifacts').listFiles().collect({ it.name }) as Set
    fileNames.size() >= 2
    fileNames.contains('commons-lang-2.5.jar')
    fileNames.contains('junit-4.11.jar')
  }

  def "should resolve a zip dependency"() {
    given:
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        dependencies: [ "test:zip-dependency:1.0@zip" ]
      }
    '''
    baseBuildFile(['xebialabs.dependency', 'java']) << writeDepTask() << """
      dependencies {
        compile 'test:zip-dependency@zip'
      }
    """
    when:
    runTasksSuccessfully('writeDeps')

    then:
    noExceptionThrown()
    def fileNames = new File(projectDir, 'artifacts').listFiles().collect({ it.name }) as Set
    fileNames.size() == 1
    fileNames.contains('zip-dependency-1.0.zip')
  }

  def "should rewrite dependencies using the managed version"() {
    given:
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        dependencies: [ "junit:junit:$junitVersion" ]
        rewrites {
          "foo:bar": "junit:junit"
        }
      }
    '''
    baseBuildFile(['xebialabs.dependency', 'java']) << writeDepTask() << """
      dependencies {
        compile 'foo:bar'
      }
    """
    when:
    runTasksSuccessfully('writeDeps')

    then:
    noExceptionThrown()
    def fileNames = new File(projectDir, 'artifacts').listFiles().collect({ it.name }) as Set
    fileNames.size() >= 1
    fileNames.contains('junit-4.12.jar')
  }

  def "should rewrite to the version of the original artifact if no managed version is found for the rewrite"() {
    given:
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        dependencies: [ "foo:bar:4.12" ]
        rewrites {
          "foo:bar": "junit:junit"
        }
      }
    '''
    baseBuildFile(['xebialabs.dependency', 'java']) << writeDepTask() << """
      dependencies {
        compile 'foo:bar'
      }
    """
    when:
    runTasksSuccessfully('writeDeps')

    then:
    noExceptionThrown()
    def fileNames = new File(projectDir, 'artifacts').listFiles().collect({ it.name }) as Set
    fileNames.size() >= 1
    fileNames.contains('junit-4.12.jar')
  }

  def "should blacklist dependencies"() {
    given:
    settingsFile << "include 'sub'"
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        dependencies: [
          "junit:junit:$junitVersion"
          "com.netflix.nebula:nebula-test:3.1.0"
        ]
        blacklist: [ "org.hamcrest:hamcrest-core", "commons-lang:commons-lang" ]
      }
    '''
    buildFile << '''
      apply plugin: 'xebialabs.dependency'

      allprojects.size() == 2
      allprojects { p ->
        apply plugin: 'java'

        repositories {
          maven {
            url "file://@repoDir@"
          }
        }

        task writeDeps(type:Copy) {
          def target = p.projectDir
          println "Copying to ${target}/artifacts"
          doFirst {
            file("$target/artifacts").mkdirs()
          }
          from configurations.compile
          into "$target/artifacts"
        }
      }
      dependencies {
        compile 'junit:junit'
      }
      subprojects {
        dependencies {
          compile 'com.netflix.nebula:nebula-test'
        }
      }
    '''.replaceAll('@repoDir@', repoDir.absolutePath)
    when:
    def result = runTasksSuccessfully(':writeDeps', ':sub:writeDeps')

    then:
    noExceptionThrown()

    def fileNames = new File(projectDir, 'artifacts').listFiles().collect({ it.name }) as Set
    fileNames.size() == 1
    !fileNames.contains('hamcrest-core-1.1.3.jar')
    fileNames.contains('junit-4.12.jar')

    def subfileNames = new File(projectDir, 'sub/artifacts').listFiles().collect({ it.name }) as Set
    subfileNames.size() == 1
    subfileNames.contains('nebula-test-3.1.0.jar')
    !subfileNames.contains('commons-lang-2.6.jar')
  }

  // Usecase is for example the $scalaVersion
  def "should resolve managed versions inside group/artifact of dependency"() {
    given:
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        versions {
          junitVersion: "4.12"
        }
        dependencies: [ "$foo:$foo:$junitVersion" ]
      }
    '''
    baseBuildFile(['xebialabs.dependency', 'java']) << writeDepTask() << """
      dependencyManagement {
        importConf project.file("reference.conf")
      }

      dependencies {
        compile "junit:\$foo"
      }
    """
    createFile("reference.conf") << '''
      dependencyManagement {
        versions {
          commonsLangVersion: "2.6"
        }
        dependencies: [ "commons-lang:commons-lang:$commonsLangVersion" ]
      }
    '''
    when:
    runTasksSuccessfully('writeDeps', '-PdependencyManagement.versions.junitVersion=4.11', '-PdependencyManagement.versions.foo=junit')

    then:
    noExceptionThrown()
    def fileNames = new File(projectDir, 'artifacts').listFiles().collect({ it.name }) as Set
    fileNames.size() >= 2
    fileNames.contains('junit-4.11.jar')
  }

  def baseBuildFile(List<String> plugins) {
    plugins.each {
      buildFile << """
        apply plugin: '$it'
      """
    }
    buildFile << """
      repositories {
        maven {
          url "file://${repoDir.absolutePath}"
        }
      }
    """
    buildFile
  }

  def writeDepTask() {
"""
      task writeDeps(type:Copy) {
        doFirst {
          file("$projectDir/artifacts").mkdirs()
        }
        from configurations.compile
        into "$projectDir/artifacts"
      }
"""
  }
}
