package com.river.connector.github.model.query

import com.river.connector.github.model.QueryParameters

class IssueQuery : QueryParameters, PageableQuery {
    enum class Filter {
        ASSIGNED, CREATED, MENTIONED, SUBSCRIBED, REPOS, ALL
    }

    enum class State {
        OPEN, CLOSED, ALL
    }

    enum class Sort {
        CREATED, UPDATED, COMMENTS
    }

    enum class Direction {
        ASC, DESC
    }

    var filter: Filter = Filter.ASSIGNED
    var state: State = State.OPEN
    var labels: String? = null
    var sort: Sort = Sort.CREATED
    var direction: Direction = Direction.DESC
    var since: String? = null
    var collab: Boolean = false
    var orgs: Boolean = false
    var owned: Boolean = false
    var pulls: Boolean = false
    override var perPage: Int = 30
    override var page: Int = 1
}
