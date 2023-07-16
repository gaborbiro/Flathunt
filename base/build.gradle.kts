plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
    implementation("org.seleniumhq.selenium:selenium-java:4.10.0")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}
