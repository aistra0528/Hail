package com.aistra.hail.ui.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    val apps = MutableLiveData<List<ApplicationInfo>>()
    val isRefreshing = MutableLiveData(false)
    private val query = MutableLiveData<String>("")
    val displayApps = MutableLiveData<List<ApplicationInfo>>()

    init {
        updateAppList()
    }

    private var refreshJob: Job? = null
    fun postQuery(query: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(500)
            this@AppsViewModel.query.postValue(query)
            updateDisplayAppList()
        }
    }

    fun updateAppList() {
        viewModelScope.launch {
            /* if (queryText.isNullOrEmpty())
                isRefreshing.postValue(true) // else delay(500) */
            isRefreshing.postValue(true)
            apps.postValue(HPackages.getInstalledApplications())
            // query.postValue(queryText)
            isRefreshing.postValue(false)
        }
    }

    fun updateDisplayAppList() {
        displayApps.value = apps.value?.let { filterList(it, query.value) }
    }

    /*  override fun onCleared() {
         refreshJob?.cancel()
     } */

    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
    private val ApplicationInfo.isAppFrozen get() = AppManager.isAppFrozen(packageName)
    private fun filterList(appList: List<ApplicationInfo>, query: String?): List<ApplicationInfo> {
        val pm = getApplication<HailApp>().packageManager
        return appList.filter {
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