package com.river.connector.github

import com.river.connector.github.internal.*
import com.river.connector.github.model.*
import com.river.connector.github.model.query.CommitQuery
import com.river.connector.github.model.query.PullRequestQuery
import com.river.connector.github.model.query.RepositoryIssueQuery
import com.river.connector.github.model.query.RepositoryQuery
import com.river.core.*
import com.river.connector.http.get
import com.river.connector.http.ofFlow
import com.river.connector.http.coSend
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer

/**
 * Retrieves a flow of all issues for the specified GitHub repository, filtered by the given criteria.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param filter A lambda expression to configure the filter for the issues.
 * @return A `Flow<Issue>` object representing the issues matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val issuesFlow = githubApi.issuesAsFlow(repositoryName) {
 *     state = "open"
 *     sort = "created"
 *     direction = "desc"
 * }
 * issuesFlow.collect { println(it) }
 * ```
 */
fun GithubApi.issuesAsFlow(
    repositoryName: String,
    parallelism: Int = 1,
    filter: RepositoryIssueQuery.() -> Unit = {}
): Flow<Issue> = paginatedFlowApi(filter, parallelism) { issues(repositoryName, it) }

/**
 * Retrieves a flow of all repositories for the specified GitHub user, filtered by the given criteria.
 *
 * @param username The GitHub username whose repositories should be retrieved.
 * @param filter A lambda expression to configure the filter for the repositories.
 * @return A `Flow<Repository>` object representing the repositories matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val username = "github_username"
 * val repositoriesFlow = githubApi.repositoriesAsFlow(username) {
 *     type = "owner"
 *     sort = "created"
 *     direction = "desc"
 * }
 * repositoriesFlow.collect { println(it) }
 * ```
 */
fun GithubApi.repositoriesAsFlow(
    username: String,
    parallelism: Int = 1,
    filter: RepositoryQuery.() -> Unit = {}
): Flow<Repository> = paginatedFlowApi(filter, parallelism) { repositories(username, it) }

/**
 * Retrieves a flow of all pull requests for the specified GitHub repository, filtered by the given criteria.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param filter A lambda expression to configure the filter for the pull requests.
 * @return A `Flow<PullRequest>` object representing the pull requests matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val pullRequestsFlow = githubApi.pullRequestsAsFlow(repositoryName) {
 *     state = "open"
 *     sort = "created"
 *     direction = "desc"
 * }
 * pullRequestsFlow.collect { println(it) }
 * ```
 */
fun GithubApi.pullRequestsAsFlow(
    repositoryName: String,
    parallelism: Int = 1,
    filter: PullRequestQuery.() -> Unit = {}
): Flow<PullRequest> = paginatedFlowApi(filter, parallelism) { pullRequests(repositoryName, it) }

/**
 * Retrieves a flow of all commits for the specified GitHub repository, filtered by the given criteria.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param filter A lambda expression to configure the filter for the commits.
 * @return A `Flow<Commit>` object representing the commits matching the specified filter criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val commitsFlow = githubApi.commitsAsFlow(repositoryName) {
 *     since = "2023-01-01T00:00:00Z"
 *     until = "2023-01-31T23:59:59Z"
 * }
 * commitsFlow.collect { println(it) }
 * ```
 */
fun GithubApi.commitsAsFlow(
    repositoryName: String,
    parallelism: Int = 1,
    filter: CommitQuery.() -> Unit = {}
): Flow<Commit> = paginatedFlowApi(filter, parallelism) {
    commits(repositoryName, it)
}

/**
 * Retrieves a flow of tree entries for the specified GitHub repository, filtered by file extensions and skipped folders.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param fileExtensions A list of file extensions to filter the tree entries by.
 * @param skipFolders A list of folder names to skip while traversing the tree.
 * @param sha The SHA of the tree to start traversing from (optional).
 * @param parallelism The number of parallel requests allowed (default is 100).
 * @return A `Flow<TreeRef.TreeEntry>` object representing the tree entries matching the specified criteria.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val fileExtensions = listOf("java", "kt")
 * val skipFolders = listOf("test", "samples")
 * val treeFlow = githubApi.treeAsFlow(
 *     repositoryName,
 *     fileExtensions,
 *     skipFolders
 * )
 * treeFlow.collect { println(it) }
 * ```
 */
fun GithubApi.treeAsFlow(
    repositoryName: String,
    fileExtensions: List<String> = emptyList(),
    skipFolders: List<String> = emptyList(),
    sha: String? = null,
    parallelism: Int = 100,
): Flow<TreeRef.TreeEntry> =
    flow {
        val channel = Channel<Pair<String, CompletableDeferred<TreeRef>>>().apply {
            receiveAsFlow()
                .onEachParallel(parallelism) { (sha, callback) ->
                    callback.complete(tree(repositoryName, sha))
                }
                .collectAsync()
        }

        val stack = mutableListOf<TreeRef>()

        stack.add(tree(repositoryName, sha ?: commit(repositoryName).sha))

        while (stack.isNotEmpty()) {
            val next = stack.removeFirst()

            emitAll(
                next.tree.filter {
                    when (it) {
                        is TreeRef.TreeEntry.Blob -> it.path.split(".").lastOrNull() in fileExtensions
                        is TreeRef.TreeEntry.Tree -> true
                    }
                }.asFlow()
            )

            val stacks =
                next.tree.filterIsInstance<TreeRef.TreeEntry.Tree>()
                    .filterNot { it.path in skipFolders }
                    .mapParallel {
                        val callback = CompletableDeferred<TreeRef>()
                        channel.send(it.sha to callback)
                        callback
                    }

            stack.addAll(stacks.awaitAll())
        }

        channel.cancel()
    }

/**
 * Downloads the archive of the specified GitHub repository.
 *
 * @param repositoryName The name of the GitHub repository.
 * @param compressionType The type of compression for the archive (ZIP or TAR). Defaults to ZIP.
 * @param ref The reference to a branch or commit for the archive. Defaults to "main".
 * @return A `Flow<ByteBuffer>` object representing the content of the repository archive.
 *
 * Example usage:
 * ```
 * val githubApi = GithubApi("your_api_key_here")
 * val repositoryName = "owner/repo_name"
 * val compressionType = CompressionType.ZIP
 * val ref = "main"
 * val archiveFlow = githubApi.downloadRepositoryArchive(repositoryName, compressionType, ref)
 * archiveFlow.collect { byteBuffer -> ... }
 * ```
 */
fun GithubApi.downloadRepositoryArchive(
    repositoryName: String,
    compressionType: CompressionType = CompressionType.ZIP,
    ref: String = "main"
): Flow<ByteBuffer> = flow {
    val url = "$baseUrl/repos/$repositoryName/${compressionType.type}/$ref"

    val request = get(url) { defaultHeaders() }

    emitAll(request.coSend(ofFlow, client).body())
}
