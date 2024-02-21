package net.opendasharchive.openarchive.core.infrastructure.model

data class VerifyDeviceIntegrityRequest(
    val token: String,
    val `package`: String,
)
