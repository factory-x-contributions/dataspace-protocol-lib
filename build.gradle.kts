plugins {
    val springBootVersion: String by System.getProperties()
    val springDependencyManagementVersion: String by System.getProperties()
    java
    `maven-publish`
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version springDependencyManagementVersion
}

java {
    val javaVersion: String by System.getProperties()
    toolchain {
        languageVersion = JavaLanguageVersion.of(Integer.parseInt(javaVersion))
    }
}

tasks {
    bootJar {
        enabled = false
    }

    jar {
        enabled = true
    }
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

