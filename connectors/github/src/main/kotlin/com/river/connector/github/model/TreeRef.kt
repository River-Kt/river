package com.river.connector.github.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class TreeRef(
    val sha: String,
    val url: String,
    val tree: List<TreeEntry>
) {
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = TreeEntry.Tree::class, name = "tree"),
        JsonSubTypes.Type(value = TreeEntry.Blob::class, name = "blob")
    )
    sealed interface TreeEntry {
        val path: String
        val mode: String
        val sha: String
        val url: String

        data class Tree(
            override val path: String,
            override val mode: String,
            override val sha: String,
            override val url: String,
        ) : TreeEntry

        data class Blob(
            override val path: String,
            override val mode: String,
            override val sha: String,
            override val url: String,
            val size: Long
        ) : TreeEntry
    }
}
