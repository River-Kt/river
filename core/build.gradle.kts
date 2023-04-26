import Dependencies.Common
import Dependencies.CommonTest

plugins {
    kotlin("jvm")
}

dependencies {
    Common.forEach { implementation(it) }
    CommonTest.forEach { testImplementation(it) }
}
