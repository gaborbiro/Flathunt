plugins {
    id("java")
}

group = "app.gaborbiro"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
}

tasks.test {
    useJUnitPlatform()
}