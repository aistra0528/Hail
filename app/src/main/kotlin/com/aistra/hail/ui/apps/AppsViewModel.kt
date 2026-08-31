package com.aistra.hail.ui.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    val apps = MutableLiveData<List<ApplicationInfo>>()
    val isRefreshing = MutableLiveData(false)
    val query = MutableLiveData("")
    val displayApps = MutableLiveData<List<ApplicationInfo>>()

    init {
        viewModelScope.launch {
            AppMetaCache.installedApplicationsReady.first { it }
            updateAppList()
        }
        updateAppList()
    }

    private var refreshJob: Job? = null
    private var refreshStateJob: Job? = null
    private var lastUpdateTime: Long = 0
    private var appListRefreshJob: Job? = null

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
    fun updateAppList(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - lastUpdateTime < 1000) return
        lastUpdateTime = now
        if (forceRefresh) {
            appListRefreshJob?.cancel()
            postRefreshState(true)
        }
        viewModelScope.launch {
            val appList = withContext(Dispatchers.IO) {
                AppMetaCache.getInstalledApplicationsCacheFirst(forceRefresh)
            }
            if (appList.isNotEmpty()) {
                apps.postValue(appList)
                updateDisplayAppList()
            }
            if (forceRefresh) {
                postRefreshState(false)
            } else if (appList.isNotEmpty()) {
                appListRefreshJob = viewModelScope.launch {
                    withContext(Dispatchers.IO) { HPackages.getInstalledApplications() }.let { refreshed ->
                        val currentPackages = apps.value?.map { it.packageName }?.toSet() ?: emptySet()
                        val newPackages = refreshed.map { it.packageName }.toSet()
                        if (currentPackages != newPackages) {
                            apps.postValue(refreshed)
                            updateDisplayAppList()
                        }
                        AppMetaCache.prefetch(refreshed)
                        AppIconCache.prefetch(getApplication(), refreshed)
                    }
                }
            }
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
                displayApps.postValue(filterList(it, query.value))
            }
        }
    }


    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM

    private suspend fun filterList(
        appList: List<ApplicationInfo>,
        query: String?
    ): List<ApplicationInfo> {
        return withContext(Dispatchers.Default) {
            return@withContext appList.filter {
                val metadata = AppMetaCache.get(it.packageName)
                val isSystemApp = metadata?.isSystemApp ?: it.isSystemApp
                val name = metadata?.name ?: it.packageName
                val frozen = metadata?.state == AppInfo.State.FROZEN
                (HailData.filterAllApps
                        || (HailData.filterUserApps && !isSystemApp)
                        || (HailData.filterSystemApps && isSystemApp))

                        && ((HailData.filterFrozenApps && frozen)
                        || (HailData.filterUnfrozenApps && !frozen))
                        // Search apps
                        && ((HailData.nineKeySearch
                        && (NineKeySearch.search(query, it.packageName, name)))
                        || FuzzySearch.search(it.packageName, query)
                        || FuzzySearch.search(name, query)
                        || PinyinSearch.searchPinyinAll(name, query))
            }.run {
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> sortedBy {
                        AppMetaCache.get(it.packageName)?.firstInstallTime ?: 0
                    }

                    HailData.SORT_UPDATE -> sortedByDescending {
                        AppMetaCache.get(it.packageName)?.lastUpdateTime ?: 0
                    }

                    else -> sortedWith(NameComparator)
                }
            }
        }
    }
}