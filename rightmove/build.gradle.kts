plugins {
    id("kotlin")
}

group = "app.gaborbiro"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data"))
    implementation(project(":service"))
    implementation(project(":base"))

    // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
    implementation("org.seleniumhq.selenium:selenium-java:4.10.0")
}
