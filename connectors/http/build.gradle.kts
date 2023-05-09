import Dependencies.Common
import Dependencies.CommonTest
import Dependencies.RiverCore

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(RiverCore)
    Common.forEach { implementation(it) }
    CommonTest.forEach { testImplementation(it) }
}
