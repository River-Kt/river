subprojects {
    apply(plugin = rootProject.libs.plugins.android.get().pluginId)

    kotlin {
        androidTarget { publishLibraryVariants("release", "debug") }

        sourceSets {
            jvmMain {
                dependencies {
                    api(rootProject.libs.aws.http)
                    api(rootProject.libs.aws.smithy.http)
                }
            }

            commonMain {
                dependencies {
                    api(rootProject.libs.aws.http)
                    api(rootProject.libs.aws.smithy.http)
                    runtimeOnly(rootProject.libs.aws.smithy.http.jvm)
                }
            }

            commonTest {
                dependencies {
                    api(rootProject.libs.mockk)
                    api(rootProject.libs.aws.smithy.http)
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
