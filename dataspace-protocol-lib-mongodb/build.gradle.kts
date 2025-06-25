
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

dependencies {
    implementation(project(":dsp-lib"))
    implementation(project(":dsp-mongodb"))
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to name,
            "Implementation-Version" to libVersion
        )
    }
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group
            artifactId = name
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
    }
}
