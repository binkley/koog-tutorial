plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.mordant.markdown)
    implementation(libs.jansi)
    implementation(libs.jline)
    implementation(libs.koog)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    runtimeOnly(libs.slf4j.simple)
}

application {
    mainClass.set("MainKt")
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
