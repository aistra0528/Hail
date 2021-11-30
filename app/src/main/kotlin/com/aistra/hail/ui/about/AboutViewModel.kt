package com.aistra.hail.ui.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.aistra.hail.BuildConfig
import java.text.SimpleDateFormat

class AboutViewModel(application: Application) : AndroidViewModel(application) {
    val time = MutableLiveData<String>().apply {
        value = SimpleDateFormat.getDateInstance().format(
            application.packageManager.getPackageInfo(
                BuildConfig.APPLICATION_ID, 0
            ).firstInstallTime
        )
    }
}