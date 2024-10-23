package org.example.noh4uk.socialRating.core.models

data class PagedList<TItem>(
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalItems: Int,
    val elements: List<TItem>
)