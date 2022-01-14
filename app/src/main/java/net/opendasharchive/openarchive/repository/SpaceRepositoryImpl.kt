package net.opendasharchive.openarchive.repository

import android.text.TextUtils
import com.orm.SugarRecord
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.Prefs

class SpaceRepositoryImpl : SpaceRepository {

    override suspend fun getAll(): List<Space>? {
        val spaceIterator = SugarRecord.findAll(Space::class.java)
        return spaceIterator?.asSequence()?.toList()
    }

    override suspend fun getCurrent(): Space? {
        val spaceId = Prefs.getCurrentSpaceId()
        if (spaceId > -1) {
            return try {
                val space: Space = SugarRecord.findById(Space::class.java, spaceId)
                if (TextUtils.isEmpty(space.name)) space.name = space.username
                space
            } catch (e: Exception) {
                //handle exception that may accure when current space id is null
                null
            }
        }
        return null
    }
}