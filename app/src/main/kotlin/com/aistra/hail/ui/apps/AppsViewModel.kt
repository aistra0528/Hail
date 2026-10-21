package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.*
import com.aistra.hail.utils.HPackages.labelString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AppsViewModel : ViewModel() {
    val apps = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    val isRefreshing = MutableStateFlow(false)
    val query = MutableStateFlow<String?>(null)
    val displayApps = MutableStateFlow<List<ApplicationInfo>>(emptyList())

    private var refreshJob: Job? = null
    private var refreshStateJob: Job? = null

    /**
     * Delaying changes to the refreshing state prevents the progress bar from flickering.
     * */
    private fun postRefreshState(state: Boolean, delayTime: Duration = 200.milliseconds) {
        if (!state) {
            refreshStateJob?.cancel()
            isRefreshing.value = false
        } else if (refreshStateJob?.isCompleted ?: true) {
            refreshStateJob = viewModelScope.launch {
                delay(delayTime)
                isRefreshing.value = true
            }
        }
    }

    fun postQuery(text: String?, delayTime: Duration = 300.milliseconds) {
        refreshJob?.cancel()
        query.value = text
        if (text.isNullOrEmpty()) updateDisplayAppList()
        else refreshJob = viewModelScope.launch {
            delay(delayTime)
            updateDisplayAppList()
        }
    }

    /**
     * This method is only used to refresh all the applications that the user has installed
     * and has no filtering or sorting effect.
     * */
    fun updateAppList() {
        viewModelScope.launch {
            postRefreshState(true)
            val lastApps = HPackages.getInstalledApplications()
            if (apps.value != lastApps) {
                apps.value = lastApps
                updateDisplayAppList()
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
        viewModelScope.launch {
            postRefreshState(true)
            displayApps.value = filterList(apps.value, query.value)
            postRefreshState(false)
        }
    }

    private suspend fun filterList(
        appList: List<ApplicationInfo>,
        query: String?
    ): List<ApplicationInfo> {
        return withContext(Dispatchers.Default) {
            return@withContext appList.filter { info ->
                when (HailData.filterType) {
                    HailData.FILTER_TYPE_SYSTEM -> HPackages.isSystemApp(info.packageName)
                    HailData.FILTER_TYPE_USER -> HPackages.isSystemApp(info.packageName).not()
                    else -> true
                } && when (HailData.filterState) {
                    HailData.FILTER_RUNNING -> HPackages.isAppStopped(info.packageName).not()
                    HailData.FILTER_SUSPENDED -> HPackages.isAppSuspended(info.packageName)
                    HailData.FILTER_DISABLED -> HPackages.isAppDisabled(info.packageName)
                    HailData.FILTER_HIDDEN -> HPackages.isAppHidden(info.packageName)
                    HailData.FILTER_UNINSTALLED -> HPackages.isAppUninstalled(info.packageName)
                    else -> true
                }
                        && ((HailData.nineKeySearch
                        && (NineKeySearch.search(query, info.packageName, info.labelString)))
                        || FuzzySearch.search(info.packageName, query)
                        || FuzzySearch.search(info.labelString, query)
                        || PinyinSearch.searchPinyinAll(info.labelString, query))
            }.run {
                when (HailData.sortOrder) {
                    HailData.SORT_INSTALL -> sortedBy {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)?.firstInstallTime ?: 0
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