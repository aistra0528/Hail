package com.aistra.hail.ui.components

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages

val grayscaleFilter = ColorMatrixColorFilter(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun AppIcon(info: ApplicationInfo?, grayscale: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(info?.packageName) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(info?.packageName) {
        if (info != null) AppIconCache.loadIconBitmapAsync(context, info, HPackages.myUserId) {
            bitmap = it
        } else bitmap = context.packageManager.defaultActivityIcon.toBitmap()
    }
    if (bitmap != null) Image(
        bitmap = bitmap!!.asImageBitmap(),
        contentDescription = null,
        modifier = modifier,
        colorFilter = if (grayscale) grayscaleFilter else null
    )
    else Spacer(modifier = modifier)
}
