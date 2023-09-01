package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppInfo
import java.text.Collator

object NameComparator : Comparator<Any> {
    private val c = Collator.getInstance()
    override fun compare(a: Any, b: Any): Int = when {
        a is ApplicationInfo && b is ApplicationInfo -> c.compare(
            a.loadLabel(app.packageManager),
            b.loadLabel(app.packageManager)
        )
        a is AppInfo && b is AppInfo -> when {
            a.pinned && !b.pinned -> -1
            b.pinned && !a.pinned -> 1
            else -> c.compare(a.name, b.name)
        }
        else -> 0
    }
}