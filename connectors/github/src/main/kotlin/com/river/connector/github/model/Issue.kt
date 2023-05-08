package com.river.connector.github.model

data class Issue(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val closedAt: String?,
    val user: User,
    val assignee: User?,
    val assignees: List<User>,
    val labels: List<Label>
) {
    data class User(
        val login: String,
        val id: Long,
        val avatarUrl: String
    )

    data class Label(
        val id: Long,
        val name: String,
        val color: String
    )
}
