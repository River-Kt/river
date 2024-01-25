package com.river.connector.github.model.query

import com.river.connector.github.model.QueryParameters
import java.time.ZonedDateTime

class CommitQuery : QueryParameters, PageableQuery {
    var sha: String? = null
    var path: String? = null
    override var page: Int = 1
    override var perPage: Int = 30
    var until: ZonedDateTime? = null
    var since: ZonedDateTime? = null
    var committer: String? = null
    var author: String? = null
}
