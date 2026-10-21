package com.aistra.hail.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.aistra.hail.R

@Composable
fun ClickableItem(title: String, desc: String? = null, icon: ImageVector? = null, onClick: () -> Unit = { }) =
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(
            imageVector = icon, contentDescription = null, modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(if (desc == null) R.dimen.padding_medium else R.dimen.padding_large)
            )
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (desc != null) Text(text = desc, style = MaterialTheme.typography.bodyMedium)
        }
    }

@Composable
fun ClickableItem(@StringRes titleId: Int, desc: String? = null, icon: ImageVector? = null, onClick: () -> Unit = { }) =
    ClickableItem(title = stringResource(titleId), desc = desc, icon = icon, onClick = onClick)

@Composable
fun ClickableItem(
    @StringRes titleId: Int,
    @StringRes descId: Int,
    icon: ImageVector? = null,
    onClick: () -> Unit = { }
) =
    ClickableItem(title = stringResource(titleId), desc = stringResource(descId), icon = icon, onClick = onClick)
