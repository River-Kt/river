import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:8.1.4")
}
