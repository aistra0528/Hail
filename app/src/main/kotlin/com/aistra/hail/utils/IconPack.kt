package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IconPack {
    @SuppressLint("DiscouragedApi")
    suspend fun loadIcon(packageName: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resources = app.packageManager.getResourcesForApplication(HailData.iconPack)
            getResourceName(resources, HailData.iconPack, packageName)?.let {
                BitmapFactory.decodeResource(
                    resources, resources.getIdentifier(it, "drawable", HailData.iconPack)
                )
            }
        }.getOrNull()
    }

    @SuppressLint("DiscouragedApi")
    private fun getResourceName(
        resources: Resources, resPackage: String, componentName: String
    ): String? {
        val parser = resources.getXml(resources.getIdentifier("appfilter", "xml", resPackage))
        while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
            runCatching {
                if (parser.eventType == XmlResourceParser.START_TAG && parser.getAttributeValue(0)
                        .contains(componentName)
                ) {
                    return parser.getAttributeValue(1)
                }
            }
            parser.next()
        }
        return null
    }
}