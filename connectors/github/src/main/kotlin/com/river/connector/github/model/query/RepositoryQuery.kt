package com.river.connector.github.model.query

import com.river.connector.github.model.QueryParameters

class RepositoryQuery : QueryParameters, PageableQuery {
    enum class Type {
        ALL, PUBLIC, PRIVATE, FORKS, SOURCES, MEMBER
    }

    enum class Sort {
        CREATED, UPDATED, PUSHED, FULL_NAME
    }

    enum class Direction {
        ASC, DESC
    }

    var type: Type = Type.ALL
    var sort: Sort = Sort.CREATED
    var direction: Direction = Direction.DESC
    override var perPage: Int = 30
    override var page: Int = 1
}
