package org.example.noh4uk.socialRating.utils


interface Permission {
    val full: String
}

enum class Permissions(override val full: String): Permission {
    All("social.rating.*"),
    Add("social.rating.add"),
    Remove("social.rating.remove"),
    History("social.rating.history"),
    Current("social.rating.current"),
    RemoveHistory("social.rating.history.remove"),
}