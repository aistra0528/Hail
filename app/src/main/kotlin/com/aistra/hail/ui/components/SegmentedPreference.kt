package com.aistra.hail.ui.components

import androidx.annotation.ArrayRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aistra.hail.app.HailData.toEntry

@Composable
fun <T> SegmentedPreference(
    value: T, onValueChange: (T) -> Unit, values: List<T>, valueToText: (T) -> String = { it.toString() }
) = Box(Modifier.background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))) {
    PrimaryTabRow(
        selectedTabIndex = values.indexOf(value),
        modifier = Modifier.padding(4.dp),
        containerColor = Color.Transparent,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(values.indexOf(value)),
                width = Dp.Unspecified,
                height = 32.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
        },
        divider = {}) {
        values.forEach {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                Tab(
                    selected = it == value,
                    onClick = { onValueChange(it) },
                    modifier = Modifier.height(32.dp).zIndex(1f),
                    text = {
                        Text(
                            text = valueToText(it),
                            color = if (it == value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    })
            }
        }
    }
}

@Composable
fun SegmentedPreference(
    value: String, onValueChange: (String) -> Unit, values: List<String>, @ArrayRes entriesId: Int
) = SegmentedPreference(
    value = value, onValueChange = onValueChange, values = values, valueToText = { it.toEntry(values, entriesId) })
