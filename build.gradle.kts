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
    runtimeOnly(libs.slf4j.simple)
}

application {
    mainClass.set("MainKt")
}
tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
