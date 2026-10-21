package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import com.aistra.hail.app.AppInfo
import com.aistra.hail.utils.HPackages.checked
import com.aistra.hail.utils.HPackages.labelString
import java.text.Collator

object NameComparator : Comparator<Any> {
    private val c = Collator.getInstance()
    override fun compare(a: Any, b: Any): Int = when {
        a is ApplicationInfo && b is ApplicationInfo -> when {
            a.checked && !b.checked -> -1
            b.checked && !a.checked -> 1
            else -> c.compare(a.labelString, b.labelString)
        }

        a is AppInfo && b is AppInfo -> when {
            a.pinned && !b.pinned -> -1
            b.pinned && !a.pinned -> 1
            else -> c.compare(a.name, b.name)
        }

        else -> 0
    }
}