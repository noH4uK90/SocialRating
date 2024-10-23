package org.example.noh4uk.socialRating.core.models

import java.util.UUID

data class PlayerRating(
    val id: UUID,
    val playerId: UUID,
    val nickname: String,
    val currentRating: Int,
    val history: List<RatingHistory>
)