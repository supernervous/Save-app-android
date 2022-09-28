package net.opendasharchive.openarchive.features.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.repository.ProjectRepository
import net.opendasharchive.openarchive.repository.ProjectRepositoryImpl
import net.opendasharchive.openarchive.repository.SpaceRepository
import net.opendasharchive.openarchive.repository.SpaceRepositoryImpl
import net.opendasharchive.openarchive.util.Prefs

class SpaceSettingsViewModel : ViewModel() {

    private val spaceRepository: SpaceRepository = SpaceRepositoryImpl()
    private val projectRepository: ProjectRepository = ProjectRepositoryImpl()

    private val _spaceList = MutableLiveData<List<Space>?>()
    val spaceList: LiveData<List<Space>?>
        get() = _spaceList

    private val _currentSpace = MutableLiveData<Space?>()
    val currentSpace: LiveData<Space?>
        get() = _currentSpace

    private val _projects = MutableLiveData<List<Project>?>()
    val projects: LiveData<List<Project>?>
        get() = _projects

    fun getAllSpace() {
        viewModelScope.launch {
            _spaceList.value = spaceRepository.getAll()
        }
    }

    fun getCurrentSpace() {
        viewModelScope.launch {
            _currentSpace.value = spaceRepository.getCurrent()
        }
    }

    fun getLatestSpace() {
        viewModelScope.launch {
            val latestSpace = spaceList.value?.lastOrNull()
            latestSpace?.let {
                _currentSpace.value = it
                Prefs.setCurrentSpaceId(it.id)
            }
        }
    }

    fun getAllProjects(spaceId: Long) {
        viewModelScope.launch {
            _projects.value = projectRepository.getAllBySpaceId(spaceId)
        }
    }


}