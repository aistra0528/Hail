package com.aistra.hail.ui.about

import android.app.Application
import android.app.Dialog
import android.content.pm.PackageManager
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HRepository
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.text.SimpleDateFormat

class AboutViewModel(val app: Application) : AndroidViewModel(app) {
    @Suppress("DEPRECATION")
    val time = MutableLiveData<String>().apply {
        value = SimpleDateFormat.getDateInstance()
            .format(
                if (HPackages.atLeastT()) app.packageManager.getPackageInfo(
                    app.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).firstInstallTime else app.packageManager.getPackageInfo(
                    app.packageName,
                    0
                ).firstInstallTime
            )
    }

    val snack = MutableLiveData<Int>()

    private fun hash(string: String): String = with(StringBuilder()) {
        MessageDigest.getInstance("SHA-256").digest(string.encodeToByteArray()).forEach {
            val hex = Integer.toHexString(0xff and it.toInt())
            if (hex.length == 1) append(0)
            append(hex)
        }
        toString()
    }

    fun codeCheck(code: String, dialog: Dialog) = viewModelScope.launch {
        dialog.show()
        val hash = hash(code + app.packageName)
        val result = HRepository.request("${HailData.URL_REDEEM_CODE}/$hash")
        dialog.cancel()
        snack.value = when {
            result is String && result.isDigitsOnly() -> {
                when (result.toInt()) {
                    0 -> R.string.msg_redeem_expired
                    1 -> {
                        HailData.setAid()
                        R.string.msg_redeem
                    }
                    else -> R.string.msg_redeem_invalid
                }
            }
            result is FileNotFoundException -> R.string.msg_redeem_invalid
            else -> R.string.msg_network_error
        }
    }
}