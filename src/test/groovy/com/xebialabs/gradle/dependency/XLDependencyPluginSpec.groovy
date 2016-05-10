package com.xebialabs.gradle.dependency

import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator

class XLDependencyPluginSpec extends IntegrationSpec {

  File artifactDir
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
    createFile('zip-dependency-1.0.zip', directory('test/zip-dependency/1.0', repoDir))

    repoDir.eachFileRecurse {
      println it
    }
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
    buildFile << """
    apply plugin: 'xebialabs.dependency'

    repositories {
      maven {
        url "file://${repoDir.getAbsolutePath()}"
      }
    }

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
    buildFile << """
      apply plugin: 'xebialabs.dependency'

      repositories {
        maven {
          url "file://${repoDir.getAbsolutePath()}"
        }
      }
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
    buildFile << """
      apply plugin: 'xebialabs.dependency'

      repositories {
        maven {
          url "file://${repoDir.getAbsolutePath()}"
        }
      }
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
    buildFile << """
      apply plugin: 'xebialabs.dependency'

      repositories {
        maven {
          url "file://${repoDir.getAbsolutePath()}"
        }
      }
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
    buildFile << """
      apply plugin: 'xebialabs.dependency'
      apply plugin: 'java'

      repositories {
        maven {
          url "file://${repoDir.absolutePath}"
        }
      }
      dependencyManagement {
        importConf project.file("dependencies.conf")
      }

      dependencies {
        compile 'commons-lang:commons-lang'
      }
      task writeDeps(type:Copy) {
        doFirst {
          file("$projectDir/artifacts").mkdirs()
        }
        from configurations.compile
        into "$projectDir/artifacts"
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
    buildFile << """
      apply plugin: 'xebialabs.dependency'
      apply plugin: 'java'

      repositories {
        maven {
          url "file://${repoDir.absolutePath}"
        }
      }
      dependencyManagement {
        importConf project.file("dependencies.conf")
      }

      dependencies {
        compile 'ch.qos.logback:logback-core'
      }
      task writeDeps(type:Copy) {
        doFirst {
          file("$projectDir/artifacts").mkdirs()
        }
        from configurations.compile
        into "$projectDir/artifacts"
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
    buildFile << """
      apply plugin: 'xebialabs.dependency'
      apply plugin: 'java'

      repositories {
        maven {
          url "file://${repoDir.absolutePath}"
        }
      }
      dependencyManagement {
        importConf project.file("reference.conf")
      }

      dependencies {
        compile 'commons-lang:commons-lang'
        compile 'junit:junit'
      }
      task writeDeps(type:Copy) {
        doFirst {
          file("$projectDir/artifacts").mkdirs()
        }
        from configurations.compile
        into "$projectDir/artifacts"
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
    buildFile << """
      apply plugin: 'xebialabs.dependency'
      apply plugin: 'java'

      repositories {
        maven {
          url "file://${repoDir.absolutePath}"
        }
      }

      dependencies {
        compile 'test:zip-dependency@zip'
      }
      task writeDeps(type:Copy) {
        doFirst {
          file("$projectDir/artifacts").mkdirs()
        }
        from configurations.compile
        into "$projectDir/artifacts"
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

  // TODO: the version does not get applied in a rewrite
  def "should rewrite dependencies"() {
    given:
    createFile("dependencies.conf", directory('gradle')) << '''
      dependencyManagement {
        dependencies: [ "junit:junit:$junitVersion" ]
        rewrites {
          "foo:bar": "junit:junit"
        }
      }
    '''
    buildFile << """
      apply plugin: 'xebialabs.dependency'
      apply plugin: 'java'

      repositories {
        maven {
          url "file://${repoDir.absolutePath}"
        }
      }

      dependencies {
        compile 'foo:bar:4.12'
      }
      task writeDeps(type:Copy) {
        doFirst {
          file("$projectDir/artifacts").mkdirs()
        }
        from configurations.compile
        into "$projectDir/artifacts"
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
}
