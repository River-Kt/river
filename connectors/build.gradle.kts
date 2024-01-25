subprojects {
    kotlin {
        sourceSets {
            commonMain {
                dependencies {
                    api(project(":core"))
                    api(rootProject.libs.slf4j)
                }
            }
        }
    }
}
