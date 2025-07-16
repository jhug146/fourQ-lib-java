plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
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