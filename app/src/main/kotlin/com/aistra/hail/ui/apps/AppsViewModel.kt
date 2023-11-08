package com.aistra.hail.ui.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    val apps = MutableLiveData<List<ApplicationInfo>>()
    val isRefreshing = MutableLiveData(false)
    val query = MutableLiveData("")
    val displayApps = MutableLiveData<List<ApplicationInfo>>()

    init {
        updateAppList()
    }

    private var refreshJob: Job? = null
    private var refreshStateJob: Job? = null

    /**
     * Delaying changes to the refreshing state prevents the progress bar from flickering.
     * */
    private fun postRefreshState(state: Boolean, delayTime: Long = 200L) {
        if (!state) {
            refreshStateJob?.cancel()
            isRefreshing.postValue(false)
        } else {
            if (refreshStateJob == null || refreshStateJob!!.isCompleted)
                refreshStateJob = viewModelScope.launch {
                    delay(delayTime)
                    this@AppsViewModel.isRefreshing.postValue(state)
                }
        }
    }

    fun postQuery(text: String, delayTime: Long = 300L) {
        refreshJob?.cancel()
        if (delayTime == 0L)
            this.query.postValue(text)
        else {
            refreshJob = viewModelScope.launch {
                delay(delayTime)
                this@AppsViewModel.query.postValue(text)
            }
        }
    }

    /**
     * This method is only used to refresh all the applications that the user has installed
     * and has no filtering or sorting effect.
     * */
    fun updateAppList() {
        viewModelScope.launch {
            postRefreshState(true)
            apps.postValue(HPackages.getInstalledApplications())
        }
    }

    /**
     * The list that the user actually sees.
     *
     * This method is different from `updateAppList()` in that it filters and rearranges the data
     * from `apps` and places it in `displayApps`.
     * */
    fun updateDisplayAppList() {
        apps.value?.let {
            viewModelScope.launch {
                Log.d("TAG", "updateDisplayAppList: ")
                postRefreshState(true)
                displayApps.postValue(filterList(it, query.value))
                postRefreshState(false)
            }
        }
    }


    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
    private val ApplicationInfo.isAppFrozen get() = AppManager.isAppFrozen(packageName)

    private suspend fun filterList(
        appList: List<ApplicationInfo>,
        query: String?
    ): List<ApplicationInfo> {
        val pm = getApplication<HailApp>().packageManager
        return withContext(Dispatchers.Default) {
            return@withContext appList.filter {
                ((HailData.filterUserApps && !it.isSystemApp)
                        || (HailData.filterSystemApps && it.isSystemApp))

                        && ((HailData.filterFrozenApps && it.isAppFrozen)
                        || (HailData.filterUnfrozenApps && !it.isAppFrozen))
                        // Search apps
                        && (query.isNullOrEmpty()
                        || it.packageName.contains(query, true)
                        || it.loadLabel(pm).toString().contains(query, true)
                        || PinyinSearch.searchPinyinAll(it.loadLabel(pm).toString(), query))
            }.run {
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> sortedBy {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)
                            ?.firstInstallTime ?: 0
                    }

                    HailData.SORT_UPDATE -> sortedByDescending {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)?.lastUpdateTime ?: 0
                    }

                    else -> sortedWith(NameComparator)
                }
            }
        }
    }
}