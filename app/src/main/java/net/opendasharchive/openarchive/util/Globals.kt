package net.opendasharchive.openarchive.util

/**
 * Created by micahjlucas on 12/15/14.
 */
object Globals {
    // EULA
    const val ASSET_EULA = "EULA"
    const val PREF_EULA_ACCEPTED = "eula.accepted"

    // request Codes used for media import and capture
    const val REQUEST_VIDEO_CAPTURE = 100
    const val REQUEST_IMAGE_CAPTURE = 101
    const val REQUEST_AUDIO_CAPTURE = 102
    const val REQUEST_FILE_IMPORT = 103

    // intent extras
    const val EXTRA_FILE_LOCATION = "archive_extra_file_location"
    const val PREF_NEXTCLOUD_USER_DATA = "next_cloud_user_data"
    const val EXTRA_CURRENT_MEDIA_ID = "archive_extra_current_media_id"
    const val EXTRA_CURRENT_PROJECT_ID = "archive_extra_current_project_id"

    // prefs
    const val PREF_FILE_KEY = "archive_pref_key"


    const val PREF_LICENSE_URL = "archive_pref_share_license_url"

    const val SITE_ARCHIVE = "archive" //Text, Audio, Photo, Video

    const val PREF_FIRST_TIME_KEY = "archive_first_key"

    const val FOLDER_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'GMT'ZZZZZ"
}