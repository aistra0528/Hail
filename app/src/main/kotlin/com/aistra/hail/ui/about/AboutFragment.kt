package com.aistra.hail.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat

class AboutFragment : MainFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    AboutScreen(HPackages.getUnhiddenPackageInfoOrNull(app.packageName)!!.firstInstallTime)
                }
            }
        }

    @Preview(showBackground = true)
    @Composable
    fun PreviewAboutScreen() = AppTheme { AboutScreen(System.currentTimeMillis()) }

    @Composable
    private fun AboutScreen(installTime: Long) {
        var openLicenseDialog by remember { mutableStateOf(false) }
        if (openLicenseDialog) LicenseDialog { openLicenseDialog = false }
        Column(
            modifier = Modifier.verticalScroll(state = rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            Card(
                onClick = { HUI.openLink(HailData.URL_WHY_FREE_SOFTWARE) },
                modifier = Modifier.height(dimensionResource(R.dimen.header_height))
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.padding_extra_small), Alignment.CenterVertically
                    ), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).background(Color.White, CircleShape),
                        contentScale = ContentScale.None
                    )
                    Text(
                        text = stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.app_slogan), style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            OutlinedCard(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium))) {
                ClickableItem(
                    icon = Icons.Outlined.Update, title = R.string.label_version, desc = HailData.VERSION
                ) { HUI.openLink(HailData.URL_RELEASES) }
                ClickableItem(
                    icon = Icons.Outlined.InstallMobile,
                    title = R.string.label_time,
                    desc = SimpleDateFormat.getDateInstance().format(installTime)
                ) { HUI.showToast("\uD83E\uDD76\uD83D\uDCA8\uD83D\uDC09") }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            OutlinedCard(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium))) {
                ClickableItem(
                    icon = Icons.AutoMirrored.Filled.Send, title = R.string.action_telegram
                ) { HUI.openLink(HailData.URL_TELEGRAM) }
                ClickableItem(
                    icon = Icons.Outlined.Group, title = R.string.action_qq
                ) { HUI.openLink(HailData.URL_QQ) }
                ClickableItem(
                    icon = Icons.Outlined.LocalMall, title = R.string.action_fdroid
                ) { HUI.openLink(HailData.URL_FDROID) }
                ClickableItem(
                    icon = Icons.Outlined.CardGiftcard, title = R.string.action_donate, onClick = ::openDonateDialog
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
            OutlinedCard(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium))) {
                ClickableItem(
                    icon = Icons.Outlined.Code, title = R.string.action_github
                ) { HUI.openLink(HailData.URL_GITHUB) }
                ClickableItem(
                    icon = Icons.Outlined.Translate, title = R.string.action_translate
                ) { HUI.openLink(HailData.URL_TRANSLATE) }
                ClickableItem(
                    icon = Icons.Outlined.Description, title = R.string.action_licenses
                ) { openLicenseDialog = true }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        }
    }

    @Composable
    private fun ClickableItem(
        icon: ImageVector, @StringRes title: Int, desc: String? = null, onClick: () -> Unit
    ) = Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, contentDescription = null, modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(if (desc == null) R.dimen.padding_medium else R.dimen.padding_large)
            )
        )
        Column {
            Text(text = stringResource(title), style = MaterialTheme.typography.bodyLarge)
            if (desc != null) Text(text = desc, style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    private fun LicenseDialog(onDismiss: () -> Unit) = AlertDialog(
        title = { Text(text = stringResource(R.string.action_licenses)) },
        text = {
            SelectionContainer {
                Text(
                    text = buildAnnotatedString {
                        val lines = resources.openRawResource(R.raw.licenses).bufferedReader().readLines()
                        lines.forEach {
                            if (it.isNotBlank()) withLink(
                                LinkAnnotation.Url(
                                    it.substringAfter(": "),
                                    TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                                )
                            ) {
                                append(it.substringBefore(": "))
                            }
                            if (it != lines.last()) append("\n\n")
                        }
                    }, modifier = Modifier.verticalScroll(state = rememberScrollState())
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.ok)) } })

    private fun openDonateDialog() {
        MaterialAlertDialogBuilder(activity).setTitle(R.string.title_donate)
            .setSingleChoiceItems(R.array.donate_payment_entries, 0) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> if (HUI.openLink(HailData.URL_ALIPAY_API).not()) {
                        HUI.openLink(HailData.URL_ALIPAY)
                    }

                    1 -> MaterialAlertDialogBuilder(activity).setTitle(R.string.title_donate)
                        .setView(ShapeableImageView(activity).apply {
                            val padding = resources.getDimensionPixelOffset(R.dimen.padding_large)
                            setPadding(0, padding, 0, padding)
                            setImageResource(R.mipmap.qr_wechat)
                        }).setPositiveButton(R.string.donate_wechat_scan) { _, _ ->
                            app.packageManager.getLaunchIntentForPackage("com.tencent.mm")?.let {
                                it.putExtra("LauncherUI.From.Scaner.Shortcut", true)
                                startActivity(it)
                            } ?: HUI.showToast(R.string.app_not_installed)
                        }.setNegativeButton(android.R.string.cancel, null).show()

                    2 -> MaterialAlertDialogBuilder(activity).setTitle(R.string.title_donate)
                        .setMessage(R.string.donate_bilibili_msg)
                        .setPositiveButton(R.string.donate_bilibili_space) { _, _ ->
                            HUI.openLink(HailData.URL_BILIBILI)
                        }.setNegativeButton(R.string.donate_bilibili_cancel, null).show()

                    3 -> HUI.openLink(HailData.URL_LIBERAPAY)
                    4 -> HUI.openLink(HailData.URL_PAYPAL)
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }
}