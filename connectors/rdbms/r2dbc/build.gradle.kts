import Dependencies.RiverCore
object Version {
    const val Kotlin = "1.7.20"
    const val Coroutine = "1.6.4"
    const val Slf4j = "1.7.36"
    const val R2dbcSpi = "1.0.0.RELEASE"
    const val H2 = "1.0.0.RELEASE"

    const val Kotest = "5.5.3"
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"

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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.Coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Version.Coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Version.Coroutine}")

    compileOnly("org.slf4j:slf4j-api:${Version.Slf4j}")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.r2dbc:r2dbc-spi:${Version.R2dbcSpi}")
    implementation("io.r2dbc:r2dbc-h2:${Version.H2}")

    Dependencies.Coroutines.forEach { implementation(it) }

    testImplementation("io.kotest:kotest-runner-junit5:${Version.Kotest}")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}
