package net.opendasharchive.openarchive.features.media.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.scal.secureshareui.controller.SiteController
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

    fun getFileList(siteController: SiteController?, space: Space) {
        viewModelScope.launch {
            if (siteController != null) {
                try {
                        withContext(Dispatchers.IO) {
                            val files = siteController.getFolders(
                                space,
                                space.host
                            )
                            _fileList.postValue(files)
                        }
                } catch (e: IOException) {
                    _fileList.value = arrayListOf()
                    e.printStackTrace()
                }
            }
        }
    }

}