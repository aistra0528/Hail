package com.aistra.hail.ui.about

import android.app.Application
import android.app.Dialog
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
    val time = MutableLiveData<String>().apply {
        value = SimpleDateFormat.getDateInstance()
            .format(HPackages.getPackageInfoOrNull(app.packageName)!!.firstInstallTime)
    }

    val snack = MutableLiveData<Int>()

    private fun hash(string: String): String = buildString {
        MessageDigest.getInstance("SHA-256").digest(string.encodeToByteArray()).forEach {
            val hex = Integer.toHexString(0xff and it.toInt())
            if (hex.length == 1) append(0)
            append(hex)
        }
    }

    fun codeCheck(code: String, dialog: Dialog) = viewModelScope.launch {
        dialog.show()
        HRepository.request("${HailData.URL_REDEEM_CODE}/${hash(code + app.packageName)}")
            .onSuccess {
                snack.value = when (it.toIntOrNull()) {
                    null -> R.string.msg_network_error
                    0 -> R.string.msg_redeem_expired
                    1 -> {
                        HailData.setAid()
                        R.string.msg_redeem
                    }
                    else -> R.string.msg_redeem_invalid
                }
            }.onFailure {
                snack.value = if (it is FileNotFoundException) R.string.msg_redeem_invalid
                else R.string.msg_network_error
            }
        dialog.dismiss()
    }
}