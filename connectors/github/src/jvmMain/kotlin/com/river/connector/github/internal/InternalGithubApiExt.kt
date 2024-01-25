package com.river.connector.github.internal

import com.fasterxml.jackson.module.kotlin.readValue
import com.river.connector.github.GithubApi
import com.river.connector.github.model.*
import com.river.connector.github.model.query.CommitQuery
import com.river.connector.github.model.query.PullRequestQuery
import com.river.connector.github.model.query.RepositoryIssueQuery
import com.river.connector.github.model.query.RepositoryQuery
import com.river.connector.http.get
import com.river.connector.http.coSend
import java.net.http.HttpResponse

/**
 * Retrieves the tree structure of a specific GitHub repository.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param sha The commit SHA to retrieve the tree for. If not provided, the latest commit is used.
 * @param recursive If set to true, the tree will be retrieved recursively, including nested trees.
 * @return A `TreeRef` object representing the tree structure of the specified repository.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val sha = "optional_commit_sha"
 * val recursive = true
 * val treeRef = githubApi.tree(repositoryName, sha, recursive)
 * println(treeRef)
 * ```
 */
internal suspend fun GithubApi.tree(
    repositoryName: String,
    sha: String? = null,
    recursive: Boolean = false
): TreeRef {
    val s = sha ?: commit(repositoryName).sha

    val request = get("$baseUrl/repos/$repositoryName/git/trees/$s") {
        header("Accept", "application/vnd.github+json")
        header("Authorization", "Bearer $apiKey")

        if (recursive) {
            query("recursive", "true")
        }
    }

    val response = request
        .coSend(HttpResponse.BodyHandlers.ofByteArray(), client)
        .body()

    return objectMapper.readValue(response)
}

/**
 * Retrieves a list of issues for the specified GitHub repository, filtered by the given criteria.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param filter A lambda expression to configure the filter for the issues.
 * @return A list of `Issue` objects matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val issues = githubApi.issues(repositoryName) {
 *     state = State.ALL
 *     sort = Sort.CREATED
 *     direction = Direction.DESC
 * }
 * issues.forEach { println(it) }
 * ```
 */
internal suspend fun GithubApi.issues(
    repositoryName: String,
    filter: RepositoryIssueQuery.() -> Unit = { }
): List<Issue> {
    val request = get("$baseUrl/repos/$repositoryName/issues") {
        val f = RepositoryIssueQuery().also(filter)

        f.asMap().forEach { (key, values) -> query(key, values) }

        header("Accept", "application/vnd.github+json")
        header("Authorization", "Bearer $apiKey")
    }

    val response =
        request
            .coSend(HttpResponse.BodyHandlers.ofByteArray(), client)
            .body()

    return objectMapper.readValue(response)
}

/**
 * Retrieves a list of repositories for the specified GitHub user, filtered by the given criteria.
 *
 * @param username The GitHub username whose repositories should be retrieved.
 * @param filter A lambda expression to configure the filter for the repositories.
 * @return A list of `Repository` objects matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val username = "github_username"
 * val repositories = githubApi.repositories(username) {
 *     type = Type.OWNER
 *     sort = Sort.CREATED
 *     direction = Direction.DESC
 * }
 * repositories.forEach { println(it) }
 * ```
 */
internal suspend fun GithubApi.repositories(
    username: String,
    filter: RepositoryQuery.() -> Unit = {}
): List<Repository> {
    val request = get("$baseUrl/users/$username/repos") {
        val f = RepositoryQuery().also(filter)
        setQueryParameters(f.asMap())
        defaultHeaders()
    }

    val response = request
        .coSend(HttpResponse.BodyHandlers.ofByteArray(), client)
        .body()

    return objectMapper.readValue(response)
}

/**
 * Retrieves a list of pull requests for the specified GitHub repository, filtered by the given criteria.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param filter A lambda expression to configure the filter for the pull requests.
 * @return A list of `PullRequest` objects matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val pullRequests = githubApi.pullRequests(repositoryName) {
 *     page = 1
 *     state = State.ALL
 *     sort = Sort.CREATED
 *     direction = Direction.DESC
 * }
 * pullRequests.forEach { println(it) }
 * ```
 */
internal suspend fun GithubApi.pullRequests(
    repositoryName: String,
    filter: PullRequestQuery.() -> Unit = {}
): List<PullRequest> {
    val request = get("$baseUrl/$repositoryName/pulls") {
        val f = PullRequestQuery().also(filter)
        setQueryParameters(f.asMap())
        defaultHeaders()
    }

    val response = request
        .coSend(HttpResponse.BodyHandlers.ofByteArray(), client)
        .body()

    return objectMapper.readValue(response)
}

/**
 * Retrieves the content of a blob object in a GitHub repository, specified by its SHA.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param sha The SHA of the blob object to retrieve.
 * @return A `BlobContent` object representing the content of the specified blob.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val sha = "blob_sha"
 * val blobContent = githubApi.blob(repositoryName, sha)
 * println(blobContent)
 * ```
 */
internal suspend fun GithubApi.blob(repositoryName: String, sha: String): BlobContent {
    val request = get("$baseUrl/repos/$repositoryName/git/blobs/$sha") {
        defaultHeaders()
    }

    val response = request
        .coSend(HttpResponse.BodyHandlers.ofByteArray(), client)
        .body()

    return objectMapper.readValue(response)
}

/**
 * Retrieves a list of commits for the specified GitHub repository, filtered by the given criteria.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param filter A lambda expression to configure the filter for the commits.
 * @return A list of `Commit` objects matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val commits = githubApi.commits(repositoryName) {
 *     page = 1
 *     since = "2022-01-01T00:00:00Z"
 *     until = "2022-12-31T23:59:59Z"
 * }
 * commits.forEach { println(it) }
 * ```
 */
internal suspend fun GithubApi.commits(repositoryName: String, filter: CommitQuery.() -> Unit = {}): List<Commit> {
    val request = get("$baseUrl/repos/$repositoryName/commits") {
        val f = CommitQuery().also(filter)
        setQueryParameters(f.asMap())
        defaultHeaders()
    }

    val response = request
        .coSend(HttpResponse.BodyHandlers.ofString(), client)
        .body()

    return objectMapper.readValue(response)
}

/**
 * Retrieves a specific commit from the specified GitHub repository.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param commitSha The SHA of the commit to retrieve. Defaults to "HEAD" for the latest commit.
 * @return A `Commit` object representing the specified commit.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val commitSha = "commit_sha"
 * val commit = githubApi.commit(repositoryName, commitSha)
 * println(commit)
 * ```
 */
internal suspend fun GithubApi.commit(
    repositoryName: String,
    commitSha: String = "HEAD"
): Commit {
    val request = get("$baseUrl/repos/$repositoryName/commits/$commitSha") {
        defaultHeaders()
    }

    val response = request
        .coSend(HttpResponse.BodyHandlers.ofString(), client)
        .body()

    return objectMapper.readValue(response)
}
