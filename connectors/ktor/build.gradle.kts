subprojects {
    apply(plugin = rootProject.libs.plugins.android.get().pluginId)

    kotlin {
        linuxX64()

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
                    api(rootProject.libs.ktor.network)
                }
            }
        }
    }
}
