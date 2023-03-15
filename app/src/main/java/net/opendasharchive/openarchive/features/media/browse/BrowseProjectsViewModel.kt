package net.opendasharchive.openarchive.features.media.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.opendasharchive.openarchive.services.SiteController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Space
import java.io.File
import java.io.IOException

class BrowseProjectsViewModel: ViewModel() {

    private val _fileList = MutableLiveData<ArrayList<File>>()
    val fileList: LiveData<ArrayList<File>>
        get() = _fileList
        val progressBarFlag = MutableLiveData(false)

    fun getFileList(siteController: SiteController?, space: Space) {
        viewModelScope.launch {
            if (siteController != null) {
                progressBarFlag.value = true
                try {
                    val value =
                        withContext(Dispatchers.IO) {
                            siteController.getFolders(space, space.host)
                        }
                    _fileList.value = value
                    progressBarFlag.value = false
                } catch (e: IOException) {
                    progressBarFlag.value = false
                    _fileList.value = arrayListOf()
                    e.printStackTrace()
                }
            }
        }
    }

}