plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data"))
    implementation(project(":service"))
    implementation(project(":base"))
    implementation(project(":usecase"))

    // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
    implementation("org.seleniumhq.selenium:selenium-java:4.10.0")
}
