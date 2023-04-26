import Dependencies.File
import Dependencies.RiverCore

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(RiverCore)
    implementation(File)
    Dependencies.Common.forEach { implementation(it) }

    Dependencies.CommonTest.forEach { testImplementation(it) }
}
