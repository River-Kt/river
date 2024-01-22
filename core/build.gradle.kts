plugins {
    alias(libs.plugins.android)
}

kotlin {
    linuxX64()
    macosArm64()
    macosX64()

    js(IR) {
        browser()
        nodejs()
    }

    androidTarget()
//        publishLibraryVariants("release", "debug")

    iosX64()
    iosArm64()
}

