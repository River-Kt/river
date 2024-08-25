//plugins {
//    alias(libs.plugins.android)
//}

subprojects {
    apply(plugin = rootProject.libs.plugins.android.get().pluginId)

    kotlin {
        androidTarget { publishLibraryVariants("release", "debug") }

        linuxX64()
        linuxArm64()

        mingwX64()

        js(IR) {
            browser {
                testTask {
                    onlyIf { !skipTests() }
                }
            }

            nodejs {
                testTask {
                    onlyIf { !skipTests() }
                }
            }
        }

        macosArm64()
        macosX64()
        iosArm64()
        tvosArm64()
        watchosArm64()

        sourceSets {
            commonMain {
                dependencies {
                    api(rootProject.libs.ktor.client)
                    api(rootProject.libs.ktor.client.serialization)
                    api(rootProject.libs.ktor.client.content.negotiation)
                    api(rootProject.libs.ktor.serialization.json)
                    api(rootProject.libs.kotlinx.serialization.json)

                    api(rootProject.libs.cryptography)
                }
            }

            androidMain {
                dependencies {
                    api(rootProject.libs.cryptography.jdk)
                    api(rootProject.libs.ktor.client.android)
                }
            }

            linuxMain {
                dependencies {
                    api(rootProject.libs.cryptography.openssl3)
                    api(rootProject.libs.ktor.client.cio)
                }
            }

            jvmMain {
                dependencies {
                    api(rootProject.libs.cryptography.jdk)
                    api(rootProject.libs.ktor.client.java)
                }
            }

            jsMain {
                dependencies {
                    api(rootProject.libs.cryptography.webcrypto)
                    api(rootProject.libs.ktor.client.js)
                }
            }

            appleMain {
                dependencies {
                    api(rootProject.libs.cryptography.apple)
                    api(rootProject.libs.ktor.client.apple)
                }
            }

            mingwMain {
                dependencies {
                    api(rootProject.libs.cryptography.openssl3)
                    api(rootProject.libs.ktor.client.windows)
                }
            }
        }
    }

    onAndroid {
        namespace = "com.river"
        compileSdk = rootProject.libs.versions.android.compile.sdk.get().toInt()

        defaultConfig {
            multiDexEnabled = true
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = true

            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    dependencies {
        coreLibraryDesugaring(rootProject.libs.desugar.jdk.libs)
    }
}
