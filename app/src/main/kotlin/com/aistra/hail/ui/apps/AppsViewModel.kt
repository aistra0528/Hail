package com.aistra.hail.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.*
import kotlinx.coroutines.*

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    val apps = MutableLiveData<List<AppInfo>>()
    val isRefreshing = MutableLiveData(false)
    val query = MutableLiveData("")
    val displayApps = MutableLiveData<List<AppInfo>>()

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
        } else if (refreshStateJob == null || refreshStateJob!!.isCompleted) {
            refreshStateJob = viewModelScope.launch {
                delay(delayTime)
                isRefreshing.postValue(true)
            }
        }
    }

    fun postQuery(text: String, delayTime: Long = 300L) {
        refreshJob?.cancel()
        if (delayTime == 0L)
            query.postValue(text)
        else {
            refreshJob = viewModelScope.launch {
                delay(delayTime)
                query.postValue(text)
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
            apps.postValue(HPackages.getInstalledApps())
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
                postRefreshState(true)
                displayApps.postValue(filterList(it, query.value))
                postRefreshState(false)
            }
        }
    }


    private val AppInfo.isSystemApp: Boolean
        get() = applicationInfo?.let { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == android.content.pm.ApplicationInfo.FLAG_SYSTEM } ?: false
    private val AppInfo.isAppFrozen get() = AppManager.isAppFrozen(packageName, userId)

    private suspend fun filterList(
        appList: List<AppInfo>,
        query: String?
    ): List<AppInfo> {
        val pm = getApplication<HailApp>().packageManager
        return withContext(Dispatchers.Default) {
            return@withContext appList.filter {
                ((HailData.filterUserApps && !it.isSystemApp)
                        || (HailData.filterSystemApps && it.isSystemApp))

                        && ((HailData.filterFrozenApps && it.isAppFrozen)
                        || (HailData.filterUnfrozenApps && !it.isAppFrozen))
                        // Search apps
                        && ((HailData.nineKeySearch
                        && (NineKeySearch.search(query, it.packageName, it.name.toString())))
                        || FuzzySearch.search(it.packageName, query)
                        || FuzzySearch.search(it.name.toString(), query)
                        || PinyinSearch.searchPinyinAll(it.name.toString(), query))
            }.run {
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> sortedBy {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName, userId = it.userId)
                            ?.firstInstallTime ?: 0
                    }

                    HailData.SORT_UPDATE -> sortedByDescending {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName, userId = it.userId)?.lastUpdateTime ?: 0
                    }

                    else -> sortedWith(NameComparator)
                }
            }
        }
    }
}
