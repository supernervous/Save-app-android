package net.opendasharchive.openarchive.features.media.browse

import android.app.Application
import androidx.lifecycle.*
import io.scal.secureshareui.controller.SiteController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.repository.ProjectRepository
import net.opendasharchive.openarchive.repository.ProjectRepositoryImpl
import net.opendasharchive.openarchive.repository.SpaceRepository
import net.opendasharchive.openarchive.repository.SpaceRepositoryImpl
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController
import net.opendasharchive.openarchive.services.webdav.WebDAVSiteController
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class BrowseProjectsViewModel(
    private val context: Application
) : ViewModel() {

    private val spaceRepository: SpaceRepository = SpaceRepositoryImpl()
    private val projectRepository: ProjectRepository = ProjectRepositoryImpl()

    private val _fileList = MutableLiveData<ArrayList<File>>()
    val fileList: LiveData<ArrayList<File>>
        get() = _fileList

    private val _space = MutableLiveData<Space>()
    val space: LiveData<Space>
        get() = _space

    fun getFileList() {
        viewModelScope.launch {
            val currentSpace = spaceRepository.getCurrent()
            currentSpace?.let {
                val siteController = when (it.type) {
                    Space.TYPE_WEBDAV -> {
                        SiteController.getSiteController(
                            WebDAVSiteController.SITE_KEY,
                            context,
                            null,
                            null
                        )
                    }
                    Space.TYPE_DROPBOX -> {
                        SiteController.getSiteController(
                            DropboxSiteController.SITE_KEY,
                            context,
                            null,
                            null
                        )
                    }
                    else -> {
                        null
                    }
                }
                siteController?.let {
                    try {
                        launch(Dispatchers.IO) {
                            _fileList.value = siteController.getFolders(
                                currentSpace,
                                currentSpace.host
                            )
                        }

                    } catch (e: IOException) {
                        _fileList.value = arrayListOf()
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun saveProjectIfNotExist(projectDescription: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val currentSpace = spaceRepository.getCurrent()
            currentSpace?.let {
                val listProjects = Project.getAllBySpace(currentSpace.id, false)
                if (listProjects?.filter { it.description == projectDescription }.isNullOrEmpty()) {
                    with(Project()) {
                        created = Date()
                        description = projectDescription
                        spaceId = Space.getCurrentSpace()?.id
                        projectRepository.saveProject(this)
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class BrowseProjectsViewModelProvider(
    private val context: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowseProjectsViewModel::class.java)) {
            return BrowseProjectsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}