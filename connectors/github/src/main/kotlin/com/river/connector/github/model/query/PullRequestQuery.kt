package com.river.connector.github.model.query

import com.river.connector.github.model.QueryParameters

class PullRequestQuery : QueryParameters, PageableQuery {
    enum class State {
        OPEN, CLOSED, ALL
    }

    enum class Sort {
        CREATED, UPDATED, POPULARITY, LONG_RUNNING
    }

    enum class Direction {
        ASC, DESC
    }

    var state: State = State.OPEN
    var head: String? = null
    var base: String? = null
    var sort: Sort = Sort.CREATED
    var direction: Direction? = null
    override var perPage: Int = 30
    override var page: Int = 1
}
