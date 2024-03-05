package net.opendasharchive.openarchive.features.internetarchive.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginScreen

class InternetArchiveActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InternetArchiveLoginScreen()
        }
    }
}
