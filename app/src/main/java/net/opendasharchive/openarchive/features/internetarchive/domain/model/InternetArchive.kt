package net.opendasharchive.openarchive.features.internetarchive.domain.model

data class InternetArchive(
    val username: String,
    val email: String,
    val expires: String,
    val auth: InternetArchiveAuth
)
