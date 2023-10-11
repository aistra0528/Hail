package com.aistra.hail.ui.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.aistra.hail.utils.HPackages
import java.text.SimpleDateFormat

class AboutViewModel(val app: Application) : AndroidViewModel(app) {
    val time = MutableLiveData<String>().apply {
        value = SimpleDateFormat.getDateInstance()
            .format(HPackages.getUnhiddenPackageInfoOrNull(app.packageName)!!.firstInstallTime)
    }
}