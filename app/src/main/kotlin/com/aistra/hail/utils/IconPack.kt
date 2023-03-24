package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailData

object IconPack {
    @SuppressLint("DiscouragedApi")
    fun loadIcon(packageName: String): Bitmap? = runCatching {
        val componentName = app.packageManager.getLaunchIntentForPackage(packageName)
            ?.run { resolveActivity(app.packageManager).toString() } ?: return null
        val resources = app.packageManager.getResourcesForApplication(HailData.iconPack)
        getResourceName(resources, HailData.iconPack, componentName)?.let {
            return BitmapFactory.decodeResource(
                resources, resources.getIdentifier(it, "drawable", HailData.iconPack)
            )
        }
    }.getOrNull()

    @SuppressLint("DiscouragedApi")
    private fun getResourceName(
        resources: Resources, packageName: String, componentName: String
    ): String? {
        val parser = resources.getXml(resources.getIdentifier("appfilter", "xml", packageName))
        while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
            runCatching {
                if (parser.eventType == XmlResourceParser.START_TAG && parser.getAttributeValue(
                        0
                    ) == componentName
                ) {
                    return parser.getAttributeValue(1)
                }
            }
            parser.next()
        }
        return null
    }
}