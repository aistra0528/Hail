package com.aistra.hail.utils

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData

object IconPack {
    fun loadIcon(packageName: String): Bitmap? {
        try {
            val componentName = HailApp.app.packageManager.getLaunchIntentForPackage(packageName)
                ?.run { resolveActivity(HailApp.app.packageManager).toString() } ?: return null
            val resources = HailApp.app.packageManager.getResourcesForApplication(HailData.iconPack)
            getResourceName(resources, HailData.iconPack, componentName)?.let {
                return BitmapFactory.decodeResource(
                    resources, resources.getIdentifier(it, "drawable", HailData.iconPack)
                )
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun getResourceName(
        resources: Resources, packageName: String, componentName: String
    ): String? {
        val parser = resources.getXml(resources.getIdentifier("appfilter", "xml", packageName))
        while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
            try {
                if (parser.eventType == XmlResourceParser.START_TAG && parser.getAttributeValue(
                        0
                    ) == componentName
                ) {
                    return parser.getAttributeValue(1)
                }
            } catch (_: Throwable) {
            }
            parser.next()
        }
        return null
    }
}