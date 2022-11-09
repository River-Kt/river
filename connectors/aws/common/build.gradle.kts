import Dependencies.RiverCore

plugins {
    id("org.jetbrains.kotlin.jvm") version Version.Kotlin

    `java-library`
}

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(RiverCore)

    Dependencies.Coroutines.forEach { implementation(it) }

    compileOnly("org.slf4j:slf4j-api:${Version.Slf4j}")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("io.kotest:kotest-runner-junit5:${Version.Kotest}")
}
