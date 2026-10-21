package com.aistra.hail.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aistra.hail.BuildConfig
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailData.showNavigationBar
import com.aistra.hail.ui.components.AppDestinations.ABOUT
import com.aistra.hail.ui.components.ClickableItem
import com.aistra.hail.ui.components.NavigateUpButton
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(
            title = { Text(text = stringResource(ABOUT.label)) },
            navigationIcon = { if (!showNavigationBar) NavigateUpButton(navController) },
            scrollBehavior = scrollBehavior
        )
    }) { contentPadding ->
        AboutContent(modifier = Modifier.fillMaxSize().padding(contentPadding))
    }
}

@Composable
fun AboutContent(modifier: Modifier = Modifier) {
    var showDonateDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    if (showDonateDialog) DonateDialog { showDonateDialog = false }
    if (showLicenseDialog) LicenseDialog { showLicenseDialog = false }
    Column(modifier = modifier.verticalScroll(state = rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            onClick = { HUI.openLink(HailData.URL_WHY_FREE_SOFTWARE) },
            modifier = Modifier.height(176.dp).padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(
                    4.dp, Alignment.CenterVertically
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
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            ClickableItem(
                icon = Icons.Outlined.Update, titleId = R.string.label_version, desc = HailData.VERSION
            ) { HUI.openLink(HailData.URL_RELEASES) }
            ClickableItem(
                icon = Icons.Outlined.InstallMobile,
                titleId = R.string.label_time,
                desc = SimpleDateFormat.getDateInstance()
                    .format(HPackages.getUnhiddenPackageInfoOrNull(BuildConfig.APPLICATION_ID)!!.firstInstallTime)
            ) { HUI.showToast("\uD83E\uDD76\uD83D\uDCA8\uD83D\uDC09") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            ClickableItem(
                icon = Icons.AutoMirrored.Filled.Send, titleId = R.string.action_telegram
            ) { HUI.openLink(HailData.URL_TELEGRAM) }
            ClickableItem(
                icon = Icons.Outlined.Group, titleId = R.string.action_qq
            ) { HUI.openLink(HailData.URL_QQ) }
            ClickableItem(
                icon = Icons.Outlined.LocalMall, titleId = R.string.action_fdroid
            ) { HUI.openLink(HailData.URL_FDROID) }
            ClickableItem(
                icon = Icons.Outlined.Coffee, titleId = R.string.action_donate
            ) { showDonateDialog = true }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            ClickableItem(
                icon = Icons.Outlined.Code, titleId = R.string.action_github
            ) { HUI.openLink(HailData.URL_GITHUB) }
            ClickableItem(
                icon = Icons.Outlined.Translate, titleId = R.string.action_translate
            ) { HUI.openLink(HailData.URL_TRANSLATE) }
            ClickableItem(
                icon = Icons.Outlined.Description, titleId = R.string.action_licenses
            ) { showLicenseDialog = true }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DonateDialog(onDismiss: () -> Unit) {
    var showWechatQRCode by remember { mutableStateOf(false) }
    if (showWechatQRCode) AlertDialog(
        onDismissRequest = { showWechatQRCode = false },
        icon = { Icon(imageVector = Icons.Outlined.Favorite, contentDescription = null) },
        title = { Text(stringResource(R.string.title_donate)) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.mipmap.qr_wechat),
                    contentDescription = stringResource(R.string.donate_wechat),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                )
            }
        },
        confirmButton = {
            val context = LocalContext.current
            Button(
                onClick = {
                    showWechatQRCode = false
                    context.packageManager.getLaunchIntentForPackage("com.tencent.mm")?.let {
                        onDismiss()
                        it.putExtra("LauncherUI.From.Scaner.Shortcut", true)
                        context.startActivity(it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    } ?: HUI.showToast(R.string.app_not_installed)
                }) { Text(stringResource(R.string.donate_wechat_scan)) }
        },
        dismissButton = {
            OutlinedButton(onClick = { showWechatQRCode = false }) { Text(stringResource(android.R.string.cancel)) }
        })
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Outlined.Favorite, contentDescription = null) },
        title = { Text(stringResource(R.string.title_donate)) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.donate_alipay to {
                    onDismiss()
                    if (HUI.openLink(HailData.URL_ALIPAY_API).not()) HUI.openLink(HailData.URL_ALIPAY)
                }, R.string.donate_wechat to {
                    showWechatQRCode = true
                }, R.string.donate_kofi to {
                    onDismiss()
                    HUI.openLink(HailData.URL_KOFI)
                }, R.string.donate_liberapay to {
                    onDismiss()
                    HUI.openLink(HailData.URL_LIBERAPAY)
                }, R.string.donate_paypal to {
                    onDismiss()
                    HUI.openLink(HailData.URL_PAYPAL)
                }).forEach { (titleId, onClick) ->
                    FilledTonalButton(onClick = { onClick() }) { Text(text = stringResource(titleId)) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseDialog(onDismiss: () -> Unit) = ModalBottomSheet(
    onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
        LocalResources.current.openRawResource(R.raw.licenses).bufferedReader().useLines { it.toList() }
            .forEach { line ->
                val list = line.split(" | ")
                ClickableItem(icon = Icons.Outlined.Link, title = list[0], desc = list[1]) {
                    HUI.openLink(list[2])
                }
            }
    }
}
