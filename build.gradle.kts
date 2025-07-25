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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.apache.logging.log4j:log4j-core:2.25.0")
    implementation("org.apache.logging.log4j:log4j-api:2.25.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}
