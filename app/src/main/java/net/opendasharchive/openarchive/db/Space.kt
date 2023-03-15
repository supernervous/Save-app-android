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
            val listSpaces = Space.getAll()
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

    enum class Type(val id: Int) {
        WEBDAV(0),
        INTERNET_ARCHIVE(1),
        DROPBOX(3)
    }

    companion object {
        fun getAll(): Iterator<Space>? {
            return findAll(Space::class.java)
        }

        fun hasSpace(type: Type, host: String, username: String): Boolean {
            return find(Space::class.java,
                "type = ? AND host = ? AND username = ?",
                type.id.toString(), host, username)
                .isNotEmpty()
        }

        fun getCurrentSpace(): Space? {
            return try {
                findById(Space::class.java, Prefs.getCurrentSpaceId())
            } catch (e2: Exception) {
                //TODO: Handle exception that may occur when current space id is null.
                null
            }
        }
    }

    val friendlyName: String
        get() {
            if (name.isNotBlank()) {
                return name
            }

            return hostUrl?.host ?: name
        }

    val hostUrl: HttpUrl?
        get() = host.toHttpUrlOrNull()

    var tType: Type?
        get() = Type.values().firstOrNull { it.id == type }
        set(value) {
            type = (value ?: Type.WEBDAV).id
        }
}