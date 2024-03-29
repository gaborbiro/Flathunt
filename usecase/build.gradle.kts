import com.anatawa12.compileTimeConstant.CreateConstantsTask
import app.gaborbiro.flathunt.Dependencies

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":usecase:base"))
    implementation(project(":base"))
    implementation(project(":data:domain"))
    implementation(project(":repo:domain"))
    implementation(project(":directions"))
    implementation(project(":console:domain"))

    implementation(Dependencies.Koin.annotations)
    implementation(Dependencies.Koin.core)
}