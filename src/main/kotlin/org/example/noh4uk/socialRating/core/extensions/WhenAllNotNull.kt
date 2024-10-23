package org.example.noh4uk.socialRating.core.extensions

fun <T: Any, R: Any> whenAllNotNull(vararg options: T?, block: (List<T>)->R) {
    if (options.all { it != null }) {
        block(options.filterNotNull())
    }
}