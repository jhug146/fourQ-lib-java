plugins {
    id("java")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// This publishing can only occur if the local gradle.properties includes the correct secrets.
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("com.namanmalhotra", "fourQ", "1.0.1")

    pom {
        name.set("FourQ Library Java")
        description.set("FourQlib is an efficient and portable cryptographic library that provides functions for computing elliptic curve based operations on the high-performance FourQ curve. This is a Java implementation that aims to minimize native performance lost.")
        inceptionYear.set("2025")
        url.set("https://github.com/malhotranaman/fourQ-lib-java")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("malhotranaman")
                name.set("Naman Malhotra")
                url.set("https://github.com/malhotranaman")
            }
            developer {
                id.set("jhug146")
                name.set("James Hughff")
                url.set("https://github.com/jhug146")
            }
        }

        scm {
            url.set("https://github.com/malhotranaman/fourQ-lib-java")
            connection.set("scm:git:git://github.com/malhotranaman/fourQ-lib-java.git")
            developerConnection.set("scm:git:ssh://git@github.com/malhotranaman/fourQ-lib-java.git")
        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jetbrains:annotations:24.1.0")

    // BouncyCastle dependencies for the K12 HashFunction
    implementation(files("lib/bcprov-jdk18on-1.81.jar"))
}

tasks.test {
    useJUnitPlatform()
}