package net.opendasharchive.openarchive.features.media.browse

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.SaveClient
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.ArrayList

class BrowseFoldersViewModel: ViewModel() {

    private val _fileList = MutableLiveData<ArrayList<File>>()
    val fileList: LiveData<ArrayList<File>>
        get() = _fileList

    val progressBarFlag = MutableLiveData(false)

    fun getFileList(context: Context, space: Space) {
        viewModelScope.launch {
            progressBarFlag.value = true

            try {
                val value = withContext(Dispatchers.IO) {
                    when (space.tType) {
                        Space.Type.WEBDAV -> getWebDavFolders(context, space)

                        Space.Type.DROPBOX -> getDropboxFolders(context, space)

                        else -> ArrayList<File>()
                    }
                }

                _fileList.value = value
                progressBarFlag.value = false
            }
            catch (e: IOException) {
                progressBarFlag.value = false
                _fileList.value = arrayListOf()

                Timber.e(e)
            }
        }
    }


    @Throws(IOException::class)
    private suspend fun getWebDavFolders(context: Context, space: Space): ArrayList<File> {
        val listFiles = ArrayList<File>()

        val baseFolderPath = StringBuffer()
            .append(space.host.replace("webdav", "dav"))
            .append("files/")
            .append(space.username)
            .append('/')
            .toString()

        SaveClient.getSardine(context, space).list(baseFolderPath)?.forEach { folder ->
            if (folder.isDirectory) {
                // This is the root folder... don't include it in the list.
                if (baseFolderPath.endsWith(folder.path)) return@forEach

                val fileFolder = File(folder.path)
                fileFolder.setLastModified(folder?.modified?.time ?: Date().time)

                listFiles.add(fileFolder)
            }
        }

        return listFiles
    }

    private suspend fun getDropboxFolders(context: Context, space: Space): ArrayList<File> {
        val client = SaveClient.getDropbox(context, space.password)

        val result = client.files().listFolder("")

        val listFiles = ArrayList<File>()

        for (md in result.entries) {
            val fileOrFolder = md.pathLower

            if (!fileOrFolder.startsWith(".")) {
                listFiles.add(File(fileOrFolder))
            }
        }

        return listFiles
    }
}