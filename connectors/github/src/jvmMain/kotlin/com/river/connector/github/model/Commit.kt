package com.river.connector.github.model

data class Commit(
    val sha: String,
    val commit: CommitDetails,
    val htmlUrl: String,
    val author: Author?,
    val committer: Committer?
) {
    data class CommitDetails(
        val message: String,
        val author: CommitAuthor,
        val committer: CommitCommitter
    )

    data class CommitAuthor(
        val name: String,
        val email: String,
        val date: String
    )

    data class CommitCommitter(
        val name: String,
        val email: String,
        val date: String
    )

    data class Author(
        val login: String?,
        val id: Long,
        val avatarUrl: String?
    )

    data class Committer(
        val login: String?,
        val id: Long,
        val avatarUrl: String?
    )
}
