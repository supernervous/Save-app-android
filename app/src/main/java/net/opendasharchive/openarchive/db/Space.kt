package net.opendasharchive.openarchive.db

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.orm.SugarRecord
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull


class SpaceChecker {
    companion object {
        fun navigateToHome(activity: AppCompatActivity) {
            val listSpaces = Space.getAllAsList()
            if (listSpaces?.hasNext() == true) {
                activity.finish()
            } else {
                val intent = Intent(activity, SpaceSetupActivity::class.java)
                activity.finishAffinity()
                activity.startActivity(intent)
            }
        }
    }
}

data class Space(
    var type: Int = 0,
    var name: String = "",
    var username: String = "",
    var password: String = "",
    var host: String = ""
) : SugarRecord() {

    companion object {
        const val TYPE_WEBDAV = 0
        const val TYPE_INTERNET_ARCHIVE = 1
        const val TYPE_DROPBOX = 3

        fun getAllAsList(): Iterator<Space>? {
            return findAll(Space::class.java)
        }

        fun getSpaceForCurrentUsername(email: String, type: Int, host:String): Int {
            var totalNoOfExistingSpaces = 0
            getAllAsList()?.asSequence()?.toList()?.let {
                totalNoOfExistingSpaces = it.count { e -> e.username == email && e.type == type && e.host == host }
            }
            return totalNoOfExistingSpaces
        }

        fun getCurrentSpace(): Space? {
            val spaceId: Long? = Prefs.getCurrentSpaceId()
            if (spaceId != null) {
                try {
                    val space: Space? = findById(Space::class.java, spaceId)
                    if (space != null) {
                        if (TextUtils.isEmpty(space.name)) space.name = space.username
                        return space
                    }
                } catch (e2: Exception) {
                    //handle exception that may accure when current space id is null
                    return null
                }
            }
            return null
        }

    }
}