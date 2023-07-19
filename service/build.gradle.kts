import com.anatawa12.compileTimeConstant.CreateConstantsTask
import dev.gaborbiro.investments.Dependencies

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("com.anatawa12.compile-time-constant")
}

apply(from = "${project.rootDir}/service.gradle")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data:domain"))
    implementation(project(":service:domain"))
    implementation(project(":repo:domain"))
    implementation(project(":base"))
    implementation(project(":usecase:base"))

    implementation(Dependencies.Selenium.java)
    implementation(Dependencies.Google.gson)
    implementation(Dependencies.Koin.core)
    implementation(Dependencies.Koin.annotations)
    ksp(Dependencies.Koin.ksp)
}


val createCompileTimeConstant = tasks.withType(CreateConstantsTask::class.java) {
    constantsClass = "app.gaborbiro.flathunt.compileTimeConstant.Constants"
    values(
        (ext["SERVICE_CONSTANTS"] as ArrayList<String>).associateBy { it }
    )
}