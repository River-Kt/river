package com.river.connector.github.model

data class Repository(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val pushedAt: String,
    val forks: Int,
    val watchers: Int,
    val owner: Owner
) {
    data class Owner(
        val login: String,
        val id: Long,
        val avatarUrl: String
    )
}
