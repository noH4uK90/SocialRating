package org.example.noh4uk.socialRating.command.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Parameter(
    val value: String,
)
