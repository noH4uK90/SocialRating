package org.example.noh4uk.socialRating.core

import org.example.noh4uk.socialRating.SocialRating
import org.example.noh4uk.socialRating.core.models.ChangingRatingType

class Utils {
    companion object {
        fun String.isNumeric() = toIntOrNull() != null

        fun getColoredType(type: ChangingRatingType): String {
            return when (type) {
                ChangingRatingType.Add -> "green"
                ChangingRatingType.Remove -> "red"
                else -> "gray"
            }
        }

        fun getRatingColor(rating: Int): String? {
            val config = SocialRating.getInstance().config
            return when {
                rating > config.getInt("upperThreshold") -> config.getString("colors.rating.high")
                rating < config.getInt("lowerThreshold") -> config.getString("colors.rating.low")
                else -> config.getString("colors.rating.normal")
            }
        }
    }
}
