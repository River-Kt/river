package com.river.connector.github.model.query

import com.river.connector.github.model.QueryParameters

class RepositoryIssueQuery : QueryParameters, PageableQuery {
    enum class State {
        OPEN, CLOSED, ALL
    }

    enum class Sort {
        CREATED, UPDATED, COMMENTS
    }

    enum class Direction {
        ASC, DESC
    }

    var milestone: String? = null
    var state: State = State.OPEN
    var assignee: String? = null
    var creator: String? = null
    var mentioned: String? = null
    var labels: String? = null
    var sort: Sort = Sort.CREATED
    var direction: Direction = Direction.DESC
    var since: String? = null
    override var perPage: Int = 30
    override var page: Int = 1
}
