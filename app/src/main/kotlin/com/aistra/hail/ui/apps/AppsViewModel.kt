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
    fun postQuery(text: String, delayTime: Long = 500L) {
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

    fun updateAppList() {
        viewModelScope.launch {
            isRefreshing.postValue(true)
            apps.postValue(HPackages.getInstalledApplications())
        }
    }

    fun updateDisplayAppList() {
        viewModelScope.launch {
            apps.value?.let {
                isRefreshing.postValue(true)
                displayApps.postValue(filterList(it, query.value))
                isRefreshing.postValue(false)
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