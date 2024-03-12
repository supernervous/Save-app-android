package net.opendasharchive.openarchive.features.integrity.infrastructure.model

data class VerifyDeviceIntegrityRequest(
    val token: String,
    val `package`: String,
)
