plugins {
    alias(libs.plugins.android)
}

kotlin {
    linuxX64()
    linuxArm64()

    mingwX64()

    js(IR) {
        browser()
        nodejs()
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
    }

    macosArm64()
    macosX64()

    iosArm64()

    tvosArm64()

    watchosArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.reflect)
            }
        }
    }
}
