plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version ("1.3.0") apply false
}

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.Coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Version.Coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Version.Coroutine}")

    compileOnly("org.slf4j:slf4j-api:${Version.Slf4j}")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("io.kotest:kotest-runner-junit5:${Version.Kotest}")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}

java.sourceSets["test"].kotlin {
    srcDir("src/sample/kotlin")
}

tasks.dokkaHtml.configure {
    dokkaSourceSets {
        configureEach {
            samples.from("src/sample/kotlin")
        }
    }
}
