
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
    implementation(project(":core"))
    implementation(project(":connectors:file"))

    Dependencies.Coroutines.forEach { implementation(it) }

    compileOnly("org.slf4j:slf4j-api:${Version.Slf4j}")

    implementation(Dependencies.CommonsNet)

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("io.kotest:kotest-runner-junit5:${Version.Kotest}")
}
