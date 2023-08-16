package com.river.connector.openai.model

import com.river.core.ExperimentalRiverApi

@ExperimentalRiverApi
data class ModelObject(
    val id: String,
    val `object`: String,
    val created: Long,
    val ownedBy: String,
    val permission: List<Permission>,
    val root: String,
    val parent: String?
)

@ExperimentalRiverApi
data class Permission(
    val id: String,
    val `object`: String,
    val created: Long,
    val allowCreateEngine: Boolean,
    val allowSampling: Boolean,
    val allowLogprobs: Boolean,
    val allowSearchIndices: Boolean,
    val allowView: Boolean,
    val allowFineTuning: Boolean,
    val organization: String,
    val group: String?,
    val isBlocking: Boolean
)
