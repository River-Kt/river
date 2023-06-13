dependencies {
    api(libs.coroutines.reactive)
    api(libs.r2dbc.spi)
    testImplementation(libs.r2dbc.h2)
}
