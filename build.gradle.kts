plugins {
    kotlin("jvm") version "2.2.20"
}

dependencies {
    implementation(libs.koog.core)
    implementation(libs.koog.gemini)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
