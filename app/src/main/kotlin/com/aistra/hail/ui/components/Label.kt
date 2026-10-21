package com.aistra.hail.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun Label(text: String) = Text(
    text = text,
    modifier = Modifier.background(
        color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)
    ).padding(horizontal = 6.dp, vertical = 2.dp),
    color = MaterialTheme.colorScheme.onSecondaryContainer,
    style = MaterialTheme.typography.labelSmall
)

@Composable
fun Label(@StringRes id: Int) = Label(stringResource(id))
