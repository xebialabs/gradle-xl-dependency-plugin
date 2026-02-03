import nebula.plugin.release.git.opinion.Strategies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.20"
    id("java")
    id("idea")
    id("maven-publish")
    id("com.github.hierynomus.license") version "0.16.1"
    id("nebula.release") version "21.0.0"
}

group = "gradle.plugin.com.xebialabs"
project.defaultTasks = listOf("build")

release {
    defaultVersionStrategy = Strategies.getSNAPSHOT()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    implementation(gradleApi())
    // localGroovy() no longer needed - all code is Java
    implementation("com.typesafe:config:1.2.1")
}

val repositoryName = if (project.version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("${project.property("nexusBaseUrl")}/repositories/${repositoryName}")
            credentials {
                username = project.property("nexusUserName").toString()
                password = project.property("nexusPassword").toString()
            }
        }
    }
}

tasks {

    register("dumpVersion") {
        doLast {
            layout.buildDirectory.asFile.get().mkdirs()
            layout.buildDirectory.file("version.dump").get().asFile.writeText("version=${project.version}")
        }
    }

    compileKotlin {
        compilerOptions{
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    compileTestKotlin {
        compilerOptions{
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    withType<ValidatePlugins>().configureEach {
        failOnWarning.set(false)
        enableStricterValidation.set(false)
    }
}
