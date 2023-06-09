import Dependencies.RiverCore

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(RiverCore)

    Dependencies.Common.forEach { implementation(it) }
    Dependencies.CommonTest.forEach { testImplementation(it) }
}
