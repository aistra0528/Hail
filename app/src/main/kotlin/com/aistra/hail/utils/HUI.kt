package com.aistra.hail.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.getSystemService
import com.aistra.hail.HailApp
import com.aistra.hail.R

object HUI {
    fun showToast(text: CharSequence, isLengthLong: Boolean = false) = Toast.makeText(
        HailApp.app, text,
        if (isLengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()

    fun showToast(resId: Int, isLengthLong: Boolean = false) =
        showToast(HailApp.app.getString(resId), isLengthLong)

    fun showToast(resId: Int, text: CharSequence, isLengthLong: Boolean = false) =
        showToast(HailApp.app.getString(resId, text), isLengthLong)

    fun startActivity(action: String = Intent.ACTION_VIEW, uri: String): Boolean = try {
        HailApp.app.startActivity(
            Intent(action, Uri.parse(uri)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (t: Throwable) {
        false
    }

    fun openLink(url: String): Boolean = startActivity(uri = url)

    fun copyText(text: String) = HailApp.app.getSystemService<ClipboardManager>()
        ?.setPrimaryClip(ClipData.newPlainText(HailApp.app.getString(R.string.app_name), text))

    fun pasteText(): String? = HailApp.app.getSystemService<ClipboardManager>()
        ?.primaryClip?.getItemAt(0)?.text?.toString()
}