package net.opendasharchive.openarchive.features.internetarchive.infrastructure.mapping

import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchive
import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchiveAuth
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginResponse

class InternetArchiveMapper {

    private operator fun invoke(response: InternetArchiveLoginResponse.S3) = InternetArchiveAuth(
        access = response.access, secret = response.secret
    )

    operator fun invoke(response: InternetArchiveLoginResponse.Values) = InternetArchive(
        userName = response.itemname ?: "",
        email = response.email ?: "",
        screenName = response.screenname ?: "",
        auth = response.s3?.let { invoke(it) } ?: InternetArchiveAuth("", "")
    )
}
