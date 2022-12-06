package io.github.gabfssilva.river.console

import io.github.gabfssilva.river.core.lines
import io.github.gabfssilva.river.core.unfold
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.*

class ConsoleExtKtTest : FeatureSpec({
    feature("Console stream") {
        scenario("Console in") {
            val inS = PipedInputStream()
            System.setIn(inS)
            val stream = PipedOutputStream(inS)

            launch {
                val writer = BufferedWriter(OutputStreamWriter(stream))

                (0..99)
                    .map { it.toString() }
                    .forEach { line ->
                        writer
                            .also { it.appendLine(line) }
                            .flush()
                    }

                writer.close()
                stream.close()
            }

            consoleIn()
                .lines()
                .withIndex()
                .take(100)
                .collect { (index, value) -> value shouldBe "$index" }
        }

        scenario("Console out") {
            val out = PipedOutputStream()
            val inS = PipedInputStream(out)
            System.setOut(PrintStream(out))

            (0..99)
                .asFlow()
                .consoleOut()
                .collect()

            out.close()

            unfold { listOf(inS.readBytes()) }
                .map { String(it) }
                .lines()
                .withIndex()
                .take(100)
                .collect { (index, value) -> value shouldBe "$index" }
        }
    }
})
