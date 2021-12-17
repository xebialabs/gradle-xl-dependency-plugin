import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
}

plugins {
    kotlin("jvm") version "1.4.20"

    id("groovy")
    id("idea")
    id("maven-publish")
    id("com.github.hierynomus.license") version "0.11.0"
    id("nebula.release") version "15.3.1"
}

group = "com.xebialabs"
project.defaultTasks = listOf("build")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

val releasedVersion = "2.0.0-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("Mdd.Hmm"))}"
project.extra.set("releasedVersion", releasedVersion)

repositories { mavenCentral() }

idea {
    module {
        setDownloadJavadoc(true)
        setDownloadSources(true)
    }
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("com.typesafe:config:1.2.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("${project.property("nexusBaseUrl")}/repositories/releases")
            credentials {
                username = project.property("nexusUserName").toString()
                password = project.property("nexusPassword").toString()
            }
        }
    }
}

tasks {
    register<NebulaRelease>("nebulaRelease")

    named<Upload>("uploadArchives") {
        dependsOn(named("publish"))
    }

    register("dumpVersion") {
        doLast {
            file(buildDir).mkdirs()
            file("$buildDir/version.dump").writeText("version=${releasedVersion}")
        }
    }

    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    }

    withType<ValidatePlugins>().configureEach {
        failOnWarning.set(false)
        enableStricterValidation.set(false)
    }
}
