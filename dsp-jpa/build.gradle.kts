plugins {
    val springBootVersion: String by System.getProperties()
    val springDependencyManagementVersion: String by System.getProperties()
    java
    `maven-publish`
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version springDependencyManagementVersion
}

group = "org.factoryx.library.connector.embedded"
version = "0.0.1-SNAPSHOT"

java {
    val javaVersion: String by System.getProperties()
    toolchain {
        languageVersion = JavaLanguageVersion.of(Integer.parseInt(javaVersion))
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(project(":dsp-lib"))
    implementation("org.springframework.boot:spring-boot-starter")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

tasks.test {
    useJUnitPlatform()
}
