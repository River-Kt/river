import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.project

object Modules {
    val core =
        Module(":core")

    val awsHttp11Spi =
        Module(":connector:connector-aws:connector-aws-java-11-http-spi")

    val http =
        Module(":connector:connector-http")

    val json =
        Module(":connector:connector-format:connector-format-json")

    val csv =
        Module(":connector:connector-format:connector-format-csv")

    val s3 =
        Module(":connector:connector-aws:connector-aws-s3")

    val sqs =
        Module(":connector:connector-aws:connector-aws-sqs")

    val sns =
        Module(":connector:connector-aws:connector-aws-sns")

    val jdbc =
        Module(":connector:connector-rdbms:connector-rdbms-jdbc")

    val file =
        Module(":connector:connector-file")
}

class Module(val name: String)

val Project.modules
    get() = Modules

fun DependencyHandler.api(module: Module): Dependency? =
    add("api", this.project(module.name))

fun DependencyHandler.implementation(module: Module): Dependency? =
    add("implementation", this.project(module.name))

fun DependencyHandler.compileOnly(module: Module): Dependency? =
    add("compileOnly", this.project(module.name))

fun DependencyHandler.testImplementation(module: Module): Dependency? =
    add("testImplementation", this.project(module.name))
