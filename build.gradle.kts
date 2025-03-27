import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import nebula.plugin.release.git.opinion.Strategies

plugins {
    kotlin("jvm") version "2.0.20"

    id("groovy")
    id("idea")
    id("maven-publish")
    id("com.github.hierynomus.license") version "0.16.1"
    id("nebula.release") version "20.2.0"
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
    implementation(localGroovy())
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

    // TODO not needed for 3.0.x
//    named<Upload>("uploadArchives") {
//        dependsOn(named("publish"))
//    }

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
