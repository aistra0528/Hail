package com.aistra.hail.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.view.WindowInsetsCompat
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R

object HUI {
    /**
     * The types of edges that the UI will avoid by default,
     * including the status bar, navigation bar, and camera area.
     * */
    val INSETS_TYPE_DEFAULT = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

    fun showToast(text: CharSequence, isLengthLong: Boolean = false) = Toast.makeText(
        app, text, if (isLengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()

    fun showToast(resId: Int, isLengthLong: Boolean = false) = showToast(app.getString(resId), isLengthLong)

    fun showToast(resId: Int, text: CharSequence, isLengthLong: Boolean = false) =
        showToast(app.getString(resId, text), isLengthLong)

    fun startActivity(action: String = Intent.ACTION_VIEW, uri: String): Boolean = runCatching {
        app.startActivity(
            Intent(action, Uri.parse(uri)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    fun openLink(url: String): Boolean = startActivity(uri = url)

    fun copyText(text: String) = app.getSystemService<ClipboardManager>()
        ?.setPrimaryClip(ClipData.newPlainText(app.getString(R.string.app_name), text))

    fun pasteText(): String? = app.getSystemService<ClipboardManager>()?.primaryClip?.getItemAt(0)?.text?.toString()
}