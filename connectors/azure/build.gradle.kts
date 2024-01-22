subprojects {
    kotlin {
        sourceSets {
            jvmMain {
                dependencies {
                    api(rootProject.libs.coroutines.reactive)
                }
            }
        }
    }
}
