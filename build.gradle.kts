plugins {
    java
    `maven-publish`
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.factoryx.library.connector.embedded"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.apache.logging.log4j:log4j-core:2.25.0")
    implementation("org.apache.logging.log4j:log4j-api:2.25.0")

    implementation("jakarta.json:jakarta.json-api:2.1.3")
    implementation("org.eclipse.parsson:parsson:1.1.7")
    implementation("com.apicatalog:titanium-json-ld:1.6.0")

    implementation("com.nimbusds:nimbus-jose-jwt:10.3")
    implementation("com.google.crypto.tink:tink:1.18.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}
