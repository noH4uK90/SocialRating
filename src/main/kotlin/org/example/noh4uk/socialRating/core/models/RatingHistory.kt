package org.example.noh4uk.socialRating.core.models

import java.util.UUID

data class RatingHistory(
    val date: String,
    val playerIdChanger: UUID,
    val playerChanger: String,
    val count: Int,
    val reason: String,
    val type: ChangingRatingType
)