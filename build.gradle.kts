plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jansi)
    implementation(libs.jline)
    implementation(libs.koog)
}

application {
    mainClass.set("MainKt")
}
tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
