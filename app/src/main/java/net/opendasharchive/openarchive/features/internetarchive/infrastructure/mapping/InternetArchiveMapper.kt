package net.opendasharchive.openarchive.features.internetarchive.infrastructure.mapping

import net.opendasharchive.openarchive.features.internetarchive.domain.model.InternetArchiveAuth
import net.opendasharchive.openarchive.features.internetarchive.infrastructure.model.InternetArchiveLoginResponse

class InternetArchiveMapper {

    fun loginToAuth(response: InternetArchiveLoginResponse) = InternetArchiveAuth(
        access = response.values.s3!!.access, secret = response.values.s3.secret
    )
}
