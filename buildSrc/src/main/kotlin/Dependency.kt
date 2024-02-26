import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project

class Modules(val project: (String) -> ProjectDependency) {
    val core by lazy { project(":core") }
    val http by lazy { project(":connector:connector-http") }
    val awsHttp11Spi by lazy { project(":connector:connector-aws:connector-aws-java-11-http-spi") }
    val debezium by lazy { project(":connector:connector-red-hat:connector-red-hat-debezium") }
    val json by lazy { project(":connector:connector-format:connector-format-json") }
    val csv by lazy { project(":connector:connector-format:connector-format-csv") }
    val s3 by lazy { project(":connector:connector-aws:connector-aws-s3") }
    val sqs by lazy { project(":connector:connector-aws:connector-aws-sqs") }
    val sns by lazy { project(":connector:connector-aws:connector-aws-sns") }
    val jdbc by lazy { project(":connector:connector-rdbms:connector-rdbms-jdbc") }
    val file by lazy { project(":connector:connector-file") }
}

val DependencyHandler.modules
    get() = Modules { project(it) }

fun modules(project: (String) -> ProjectDependency) = Modules(project)

fun DependencyHandler.api(module: Module): Dependency? =
    add("api", this.project(module.name))

fun DependencyHandler.implementation(module: Module): Dependency? =
    add("implementation", this.project(module.name))

fun DependencyHandler.compileOnly(module: Module): Dependency? =
    add("compileOnly", this.project(module.name))

fun DependencyHandler.testImplementation(module: Module): Dependency? =
    add("testImplementation", this.project(module.name))

fun <T> DependencyHandlerScope.coreLibraryDesugaring(dependencyNotation: org.gradle.api.provider.Provider<T>) =
    add("coreLibraryDesugaring", dependencyNotation)
