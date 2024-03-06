package net.opendasharchive.openarchive.features.internetarchive.domain.model

data class InternetArchive(
    val userName: String,
    val screenName: String,
    val email: String,
    val auth: InternetArchiveAuth
)
