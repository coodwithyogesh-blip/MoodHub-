package com.example.mesh

/**
 * Owner-controlled promotional card shown in Status.
 * The click target is an HTTPS URL and is opened only after the user taps the card.
 */
data class OwnerStatusAd(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val clickUrl: String,
    val enabled: Boolean = true
)
