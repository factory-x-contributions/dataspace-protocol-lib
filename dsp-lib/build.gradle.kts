val group: String by System.getProperties()
val libVersion: String by System.getProperties()

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

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.data:spring-data-commons:3.5.3")

    implementation("org.apache.logging.log4j:log4j-api:2.25.1")
    implementation("org.apache.logging.log4j:log4j-core:2.25.1")

    implementation("jakarta.json:jakarta.json-api:2.1.3")
    implementation("org.eclipse.parsson:parsson:1.1.7")
    implementation("com.apicatalog:titanium-json-ld:1.6.0")

    implementation("com.nimbusds:nimbus-jose-jwt:10.5")
    implementation("com.google.crypto.tink:tink:1.18.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.81")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }

    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testImplementation("org.testcontainers:vault:1.21.3")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")

    testImplementation("org.hsqldb:hsqldb:2.7.4")
    testImplementation(project(":dataspace-protocol-lib-sql"))
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

tasks {
    bootJar {
        enabled = false
    }

    jar {
        enabled = true
    }

    withType<Test> {
        useJUnitPlatform()
        jvmArgs("-javaagent:${mockitoAgent.asPath}")
    }
}

tasks.test {
    systemProperty("testcontainer.tck.disable", System.getProperty("testcontainer.tck.disable", "true"))
    systemProperty("testcontainer.fxint.dim.disable", System.getProperty("testcontainer.fxint.dim.disable", "true"))
}
