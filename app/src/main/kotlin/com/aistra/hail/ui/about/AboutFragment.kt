package com.aistra.hail.ui.about

import android.os.Bundle
import android.text.InputFilter
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.databinding.FragmentAboutBinding
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView

class AboutFragment : MainFragment(), View.OnClickListener {
    private lateinit var aboutViewModel: AboutViewModel
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        binding.descVersion.text = HailData.VERSION
        aboutViewModel = ViewModelProvider(this)[AboutViewModel::class.java]
        aboutViewModel.time.observe(viewLifecycleOwner) {
            binding.descTime.text = it
        }
        aboutViewModel.snack.observe(viewLifecycleOwner) {
            if (it != 0) {
                Snackbar.make(activity.fab, it, Snackbar.LENGTH_LONG).show()
                aboutViewModel.snack.value = 0
            }
        }
        binding.actionLibre.setOnClickListener(this)
        binding.actionVersion.setOnClickListener(this)
        binding.actionTime.setOnClickListener(this)
        binding.actionTelegram.setOnClickListener(this)
        binding.actionQq.setOnClickListener(this)
        binding.actionCoolapk.setOnClickListener(this)
        binding.actionDonate.setOnClickListener(this)
        binding.actionGithub.setOnClickListener(this)
        binding.actionTranslate.setOnClickListener(this)
        binding.actionLicenses.setOnClickListener(this)
        return binding.root
    }

    override fun onClick(view: View) {
        when (view) {
            binding.actionLibre -> HUI.openLink(HailData.URL_WHY_FREE_SOFTWARE)
            binding.actionVersion -> HUI.openLink(HailData.URL_RELEASES)
            binding.actionTime -> onRedeem()
            binding.actionTelegram -> HUI.openLink(HailData.URL_TELEGRAM)
            binding.actionQq -> HUI.openLink(HailData.URL_QQ)
            binding.actionCoolapk -> HUI.openLink(HailData.URL_COOLAPK)
            binding.actionDonate -> onDonate()
            binding.actionGithub -> HUI.openLink(HailData.URL_GITHUB)
            binding.actionTranslate -> HUI.openLink(HailData.URL_TRANSLATE)
            binding.actionLicenses -> MaterialAlertDialogBuilder(activity).setTitle(R.string.action_licenses)
                .setView(MaterialTextView(activity).apply {
                    val padding = resources.getDimensionPixelOffset(R.dimen.dialog_padding)
                    setPadding(padding, 0, padding, 0)
                    text = resources.openRawResource(R.raw.licenses).bufferedReader().readText()
                    Linkify.addLinks(this, Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS)
                }).setPositiveButton(android.R.string.ok, null).show()
        }
    }

    private fun onDonate() {
        MaterialAlertDialogBuilder(activity).setTitle(R.string.title_donate)
            .setSingleChoiceItems(R.array.donate_payment_entries, 0) { dialog, which ->
                dialog.cancel()
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

    private fun onRedeem() {
        if (HailData.isDeviceAid) {
            aboutViewModel.snack.value = R.string.msg_redeem
            return
        }
        val input = DialogInputBinding.inflate(layoutInflater, FrameLayout(activity), true)
        input.inputLayout.setHint(R.string.action_redeem)
        input.editText.filters = arrayOf(InputFilter.LengthFilter(64))
        MaterialAlertDialogBuilder(activity).setView(input.root.parent as View)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val progress =
                    layoutInflater.inflate(R.layout.dialog_progress, FrameLayout(activity), true)
                val dialog =
                    MaterialAlertDialogBuilder(activity).setView(progress).setCancelable(false)
                        .create()
                aboutViewModel.codeCheck(input.editText.text.toString(), dialog)
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}