package com.river.connector.github.model


data class PullRequest(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val closedAt: String?,
    val mergedAt: String?,
    val user: User,
    val assignee: User?,
    val assignees: List<User>,
    val requestedReviewers: List<User>,
    val head: Branch,
    val base: Branch
) {
    data class User(
        val login: String,
        val id: Long,
        val avatarUrl: String
    )

    data class Branch(
        val label: String,
        val ref: String,
        val sha: String
    )
}
