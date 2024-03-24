package com.aistra.hail.ui.about

import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.FragmentAboutBinding
import com.aistra.hail.extensions.*
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import dev.chrisbanes.insetter.applyInsetter

class AboutFragment : MainFragment(), View.OnClickListener {
    private lateinit var aboutViewModel: AboutViewModel
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        activity.appbar.setLiftOnScrollTargetView(binding.root)

        binding.descVersion.text = HailData.VERSION
        aboutViewModel = ViewModelProvider(this)[AboutViewModel::class.java]
        aboutViewModel.time.observe(viewLifecycleOwner) {
            binding.descTime.text = it
        }
        binding.actionLibre.setOnClickListener(this)
        binding.actionVersion.setOnClickListener(this)
        binding.actionTime.setOnClickListener(this)
        binding.actionTelegram.setOnClickListener(this)
        binding.actionQq.setOnClickListener(this)
        binding.actionFdroid.setOnClickListener(this)
        binding.actionDonate.setOnClickListener(this)
        binding.actionGithub.setOnClickListener(this)
        binding.actionTranslate.setOnClickListener(this)
        binding.actionLicenses.setOnClickListener(this)

        binding.scrollView.applyDefaultInsetter {
            paddingRelative(isRtl, bottom = isLandscape)
            marginRelative(isRtl, start = !isLandscape, end = true)
        }

        return binding.root
    }

    override fun onClick(view: View) {
        when (view) {
            binding.actionLibre -> HUI.openLink(HailData.URL_WHY_FREE_SOFTWARE)
            binding.actionVersion -> HUI.openLink(HailData.URL_RELEASES)
            binding.actionTime -> HUI.showToast("\uD83E\uDD76\uD83D\uDCA8\uD83D\uDC09")
            binding.actionTelegram -> HUI.openLink(HailData.URL_TELEGRAM)
            binding.actionQq -> HUI.openLink(HailData.URL_QQ)
            binding.actionFdroid -> HUI.openLink(HailData.URL_FDROID)
            binding.actionDonate -> onDonate()
            binding.actionGithub -> HUI.openLink(HailData.URL_GITHUB)
            binding.actionTranslate -> HUI.openLink(HailData.URL_TRANSLATE)
            binding.actionLicenses -> MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.action_licenses)
                .setMessage(resources.openRawResource(R.raw.licenses).bufferedReader().readText())
                .setPositiveButton(android.R.string.ok, null)
                .show()
                .findViewById<MaterialTextView>(android.R.id.message)?.apply {
                    setTextIsSelectable(true)
                    Linkify.addLinks(this, Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS)
                    // The first time the link is clicked the background does not change color and
                    // the view needs to get focus once.
                    requestFocus()
                }
        }
    }

    private fun onDonate() {
        MaterialAlertDialogBuilder(activity).setTitle(R.string.title_donate)
            .setSingleChoiceItems(R.array.donate_payment_entries, 0) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> if (HUI.openLink(HailData.URL_ALIPAY_API).not()) {
                        HUI.openLink(HailData.URL_ALIPAY)
                    }

                    1 -> MaterialAlertDialogBuilder(activity).setTitle(R.string.title_donate)
                        .setView(ShapeableImageView(activity).apply {
                            val padding = resources.getDimensionPixelOffset(R.dimen.dialog_padding)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}