package org.example.noh4uk.socialRating.utils

import org.example.noh4uk.socialRating.SocialRating
import org.example.noh4uk.socialRating.models.ChangingRatingType

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
                rating < config.getInt("lessThan1") -> config.getString("colors.rating.lessThan1")
                rating < config.getInt("lessThan2") -> config.getString("colors.rating.lessThan2")
                rating < config.getInt("lessThan3") -> config.getString("colors.rating.lessThan3")
                rating < config.getInt("lessThan4") -> config.getString("colors.rating.lessThan4")
                rating < config.getInt("lessThan5") -> config.getString("colors.rating.lessThan5")
                else -> config.getString("colors.rating.moreThan5")
            }
        }
    }
}
