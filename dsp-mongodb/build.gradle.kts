val group: String by System.getProperties()
val libVersion: String by System.getProperties()
val name = "dataspace-protocol-lib-mongodb"

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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation(project(":dsp-lib"))
    implementation("org.springframework.boot:spring-boot-starter")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            artifactId = "dsp-mongodb"
            version = project.version.toString()
            from(components["java"])
        }
    }
}