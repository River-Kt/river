import Dependencies.AwsHttp11Spi
import Dependencies.ConnectorCommon
import Dependencies.RiverCore

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
    ConnectorCommon.forEach { implementation(it) }

    implementation(AwsHttp11Spi)
    implementation(Dependencies.Aws.S3){
        exclude("software.amazon.awssdk", "netty-nio-client")
    }

    Dependencies.CommonTest.forEach { testImplementation(it) }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}
