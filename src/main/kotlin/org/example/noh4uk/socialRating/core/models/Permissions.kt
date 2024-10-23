package org.example.noh4uk.socialRating.core.models

enum class Permissions(val full: String) {
    All("social.rating.*"),
    Add("social.rating.add"),
    Remove("social.rating.remove"),
    History("social.rating.history"),
    Current("social.rating.current"),
}