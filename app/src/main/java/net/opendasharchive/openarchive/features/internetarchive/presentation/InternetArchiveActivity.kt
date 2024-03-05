package net.opendasharchive.openarchive.features.internetarchive.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.getSpace
import net.opendasharchive.openarchive.services.internetarchive.InternetArchiveFragment.Companion.ARG_VAL_NEW_SPACE

class InternetArchiveActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (space, isNewSpace) = intent.extras.getSpace(Space.Type.INTERNET_ARCHIVE)

        setContent {
            if (isNewSpace) {
                InternetArchiveLoginScreen(space)
            } else {
                InternetArchiveScreen(space)
            }
        }
    }
}
