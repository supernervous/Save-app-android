package net.opendasharchive.openarchive.features.internetarchive.infrastructure.mapping

import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchiveAuth
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginResponse

class InternetArchiveMapper {

    private fun toAuth(response: InternetArchiveLoginResponse.S3) = InternetArchiveAuth(
        access = response.access, secret = response.secret
    )

    fun toDomain(response: InternetArchiveLoginResponse.Values) = InternetArchive(
        username = response.screenname ?: response.itemname ?: "",
        email = response.email ?: "",
        expires = response.expires ?: "",
        auth = response.s3?.let { toAuth(it) } ?: InternetArchiveAuth("", "")
    )
}
