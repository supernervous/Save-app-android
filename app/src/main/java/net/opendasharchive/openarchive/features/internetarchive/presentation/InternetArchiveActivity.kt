package net.opendasharchive.openarchive.features.internetarchive.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.getSpace
import net.opendasharchive.openarchive.features.main.MainActivity

class InternetArchiveActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (space, isNewSpace) = intent.extras.getSpace(Space.Type.INTERNET_ARCHIVE)

        setContent {
            if (isNewSpace) {
                InternetArchiveLoginScreen(space) {
                    finish(it)
                }
            } else {
                InternetArchiveScreen(space) {
                    finish(it)
                }
            }
        }
    }

    private fun finish(result: String) {
        when(result) {
            RESP_SAVED -> startActivity(Intent(this, MainActivity::class.java))
            RESP_CANCEL -> Space.navigate(this)
        }
    }
}
