plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":core"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.Coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Version.Coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Version.Coroutine}")

    compileOnly("org.slf4j:slf4j-api:${Version.Slf4j}")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("io.kotest:kotest-runner-junit5:${Version.Kotest}")
}
