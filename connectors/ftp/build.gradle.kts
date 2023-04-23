import Dependencies.Common
import Dependencies.CommonTest
import Dependencies.RiverCore
import Dependencies.File

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(RiverCore)
    implementation(File)
    Common.forEach { implementation(it) }
    implementation(Dependencies.CommonsNet)

    testImplementation(Dependencies.MockFtpServer)
    CommonTest.forEach { testImplementation(it) }
}
