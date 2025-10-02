val group: String by System.getProperties()
val libVersion: String by System.getProperties()
val artifactName = "dataspace-protocol-lib-mongodb"

plugins {
    val springBootVersion: String by System.getProperties()
    val springDependencyManagementVersion: String by System.getProperties()
    java
    `maven-publish`
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version springDependencyManagementVersion
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation(project(":dsp-lib"))
    implementation("org.springframework.boot:spring-boot-starter")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

tasks {
    bootJar {
        enabled = false
    }

    jar {
        enabled = true
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to artifactName,
            "Implementation-Version" to libVersion
        )
    }
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/INDEX.LIST")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group
            artifactId = artifactName
            version = libVersion

            artifacts.clear()

            artifact(tasks.jar) {
                classifier = null
            }

            pom.withXml {
                val rootNode = asNode()
                val dependenciesNode = rootNode.children().find { (it as? groovy.util.Node)?.name() == "dependencies" } as? groovy.util.Node
                dependenciesNode?.let { rootNode.remove(it) }
            }
        }
    }
    repositories {
        mavenLocal()

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/factory-x-contributions/dataspace-protocol-lib")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
