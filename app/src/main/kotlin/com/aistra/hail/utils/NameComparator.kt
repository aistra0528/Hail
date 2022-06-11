package com.aistra.hail.utils

import android.content.pm.PackageInfo
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import java.text.Collator

object NameComparator : Comparator<Any> {
    private val c = Collator.getInstance()
    override fun compare(a: Any, b: Any): Int = when {
        a is PackageInfo && b is PackageInfo -> c.compare(
            a.applicationInfo.loadLabel(HailApp.app.packageManager),
            b.applicationInfo.loadLabel(HailApp.app.packageManager)
        )
        a is AppInfo && b is AppInfo -> when {
            a.pinned && !b.pinned -> -1
            b.pinned && !a.pinned -> 1
            else -> c.compare(a.name, b.name)
        }
        else -> 0
    }
}