package com.river.connector.github.model.query

import com.river.connector.github.model.QueryParameters

interface PageableQuery : QueryParameters {
    var page: Int
    var perPage: Int
}
