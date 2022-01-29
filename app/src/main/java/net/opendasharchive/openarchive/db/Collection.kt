package net.opendasharchive.openarchive.db

import com.orm.SugarRecord
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING
import java.util.Date

data class Collection(
        var projectId: Long? = null,
        var uploadDate: Date? = null,
        var serverUrl: String? = null
) : SugarRecord() {

    companion object {

        fun getAllAsList(): List<Collection>? {
            return find(Collection::class.java, EMPTY_STRING, arrayOf(), EMPTY_STRING, "ID DESC", EMPTY_STRING)
        }
    }

}