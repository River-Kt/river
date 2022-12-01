import Dependencies.AwsCommon
import Dependencies.RiverCore

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
    implementation(AwsCommon)
    implementation(Dependencies.Aws.SesV2){
        exclude("software.amazon.awssdk", "netty-nio-client")
    }

    Dependencies.Coroutines.forEach { implementation(it) }

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
