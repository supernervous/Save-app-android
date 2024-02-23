package net.opendasharchive.openarchive.core.infrastructure.model

data class VerifyDeviceIntegrityResponse(
    val success: Boolean,
    val message: String,
    val actions: Actions,
) {
    data class Actions(
        val canRefresh: Boolean,
        val showDialog: Int? = null,
        val stopApp: Boolean
    )
}