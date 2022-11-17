import nebula.plugin.release.git.opinion.Strategies

plugins {
    kotlin("jvm") version "1.6.21"

    id("groovy")
    id("idea")
    id("maven-publish")
    id("com.github.hierynomus.license") version "0.16.1"
    id("nebula.release") version "17.1.0"
}

group = "gradle.plugin.com.xebialabs"
project.defaultTasks = listOf("build")

release {
    defaultVersionStrategy = Strategies.getSNAPSHOT()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

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
            file(buildDir).mkdirs()
            file("$buildDir/version.dump").writeText("version=${project.version}")
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
