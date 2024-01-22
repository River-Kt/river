package com.river.connector.github

import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.collectIndexed

class GithubApiExtKtTest : FeatureSpec({
    val wiremock = WireMockServer()
    listener(WireMockListener(wiremock, ListenerMode.PER_SPEC))

    val github by lazy {
        GithubApi(
            apiKey = "x",
            baseUrl = "http://localhost:${wiremock.port()}"
        )
    }

    val repo = "owner/repo"

    feature("GitHub as flow") {
        scenario("Commits as flow") {
            (1..3).forEach { page ->
                wiremock
                    .get {
                        url contains "/repos/$repo/commits"
                        queryParams contains "page" equalTo "$page"
                    }
                    .returnsJson {
                        body = fileContent("/commits/page_$page.json")
                    }
            }

            github
                .commitsAsFlow(repo)
                .collectIndexed { index, value ->
                    val n = index + 1
                    value.sha shouldBe "sha-$n"
                }
        }

        //TODO: the other flows
    }
})

fun fileContent(name: String): String =
    checkNotNull(
        (Any::class as Any).javaClass
            .getResource(name)
            ?.readText()
    )
