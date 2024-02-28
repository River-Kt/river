import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

inline fun Project.onAndroid(crossinline configure: LibraryExtension.() -> Unit) {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension> { configure() }
    }
}

inline fun Project.androidApp(crossinline configure: AppExtension.() -> Unit) {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<AppExtension> { configure() }
    }
}

fun Project.skipTests(): Boolean =
    "${properties["skipTests"] ?: "false"}".toBooleanStrict()
