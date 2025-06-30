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
    implementation(files("C:/Users/andre/Downloads/lcrypto-jdk18on-1.81/lcrypto-jdk18on-1.81/src"))
}

tasks.test {
    useJUnitPlatform()
}