import Dependencies.CommonTest
import Dependencies.CoroutinesCore
import Dependencies.Slf4j

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(CoroutinesCore)
    implementation(Slf4j)
    CommonTest.forEach { testImplementation(it) }
}
