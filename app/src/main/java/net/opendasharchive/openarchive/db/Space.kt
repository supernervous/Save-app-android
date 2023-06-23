package net.opendasharchive.openarchive.db

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.amulyakhare.textdrawable.TextDrawable
import com.github.abdularis.civ.AvatarImageView
import com.orm.SugarRecord
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.services.dropbox.DropboxConduit
import net.opendasharchive.openarchive.services.internetarchive.IaConduit
import net.opendasharchive.openarchive.util.Prefs
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.*


data class Space(
    var type: Int = 0,
    var name: String = "",
    var username: String = "",
    var password: String = "",
    var host: String = ""
) : SugarRecord() {

    constructor(type: Type) : this() {
        tType = type

        when (type) {
            Type.WEBDAV -> {}
            Type.INTERNET_ARCHIVE -> {
                name = IaConduit.NAME
                host = IaConduit.ARCHIVE_BASE_URL
            }
            Type.DROPBOX -> {
                name = DropboxConduit.NAME
                host = DropboxConduit.HOST
                username = DropboxConduit.HOST
            }
        }
    }

    enum class Type(val id: Int) {
        WEBDAV(0),
        INTERNET_ARCHIVE(1),
        DROPBOX(3)
    }

    companion object {
        fun getAll(): Iterator<Space> {
            return findAll(Space::class.java)
        }

        fun get(type: Type, host: String? = null, username: String? = null): List<Space> {
            var whereClause = "type = ?"
            val whereArgs = mutableListOf(type.id.toString())

            if (!host.isNullOrEmpty()) {
                whereClause = "$whereClause AND host = ?"
                whereArgs.add(host)
            }

            if (!username.isNullOrEmpty()) {
                whereClause = "$whereClause AND username = ?"
                whereArgs.add(username)
            }

            return find(Space::class.java, whereClause, whereArgs.toTypedArray(),
                null, null, null)
        }

        fun has(type: Type, host: String? = null, username: String? = null): Boolean {
            return get(type, host, username).isNotEmpty()
        }

        var current: Space?
            get() = get(Prefs.currentSpaceId) ?: this.getAll().asSequence().firstOrNull()
            set(value) {
                Prefs.currentSpaceId = value?.id ?: -1
            }

        fun get(id: Long): Space? {
            return findById(Space::class.java, id)
        }

        fun navigate(activity: AppCompatActivity) {
            if (getAll().hasNext()) {
                activity.finish()
            }
            else {
                activity.finishAffinity()
                activity.startActivity(Intent(activity, SpaceSetupActivity::class.java))
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

    val initial: String
        get() = (friendlyName.firstOrNull() ?: 'X').uppercase(Locale.getDefault())

    val hostUrl: HttpUrl?
        get() = host.toHttpUrlOrNull()

    var tType: Type?
        get() = Type.values().firstOrNull { it.id == type }
        set(value) {
            type = (value ?: Type.WEBDAV).id
        }

    val projects: List<Project>
        get() = find(Project::class.java, "space_id = ?", arrayOf(id.toString()), null, "id DESC", null)

    fun getAvatar(context: Context): Drawable? {
        return when (tType) {
            Type.INTERNET_ARCHIVE -> ContextCompat.getDrawable(context, R.drawable.ialogo128)

            Type.DROPBOX -> ContextCompat.getDrawable(context, R.drawable.dropbox)

            else -> TextDrawable.builder().buildRound(initial,
                ContextCompat.getColor(context, R.color.colorPrimary))
        }
    }

    fun setAvatar(view: ImageView) {
        when (tType) {
            Type.INTERNET_ARCHIVE, Type.DROPBOX -> {
                if (view is AvatarImageView) {
                    view.state = AvatarImageView.SHOW_IMAGE
                }

                view.setImageDrawable(getAvatar(view.context))
            }

            else -> {
                if (view is AvatarImageView) {
                    view.state = AvatarImageView.SHOW_INITIAL
                    view.setText(initial)
                    view.avatarBackgroundColor = ContextCompat.getColor(view.context, R.color.colorPrimary)
                }
                else {
                    view.setImageDrawable(getAvatar(view.context))
                }
            }
        }
    }

    override fun delete(): Boolean {
        projects.forEach {
            it.delete()
        }

        return super.delete()
    }
}