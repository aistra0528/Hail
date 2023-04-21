package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig

object HailApi {
    /** @since 0.5.0 */
    const val ACTION_LAUNCH = "${BuildConfig.APPLICATION_ID}.action.LAUNCH"

    /** @since 0.5.0 */
    const val ACTION_FREEZE = "${BuildConfig.APPLICATION_ID}.action.FREEZE"

    /** @since 0.5.0 */
    const val ACTION_UNFREEZE = "${BuildConfig.APPLICATION_ID}.action.UNFREEZE"

    /** @since 1.1.0 */
    const val ACTION_FREEZE_TAG = "${BuildConfig.APPLICATION_ID}.action.FREEZE_TAG"

    /** @since 1.1.0 */
    const val ACTION_UNFREEZE_TAG = "${BuildConfig.APPLICATION_ID}.action.UNFREEZE_TAG"

    /** @since 0.5.0 */
    const val ACTION_FREEZE_ALL = "${BuildConfig.APPLICATION_ID}.action.FREEZE_ALL"

    /** @since 0.5.0 */
    const val ACTION_UNFREEZE_ALL = "${BuildConfig.APPLICATION_ID}.action.UNFREEZE_ALL"

    /** @since 1.0.0 */
    const val ACTION_FREEZE_NON_WHITELISTED =
        "${BuildConfig.APPLICATION_ID}.action.FREEZE_NON_WHITELISTED"

    /** @since 1.3.0 */
    const val ACTION_FREEZE_AUTO = "${BuildConfig.APPLICATION_ID}.action.FREEZE_AUTO"

    /** @since 0.6.0 */
    const val ACTION_LOCK = "${BuildConfig.APPLICATION_ID}.action.LOCK"

    /** @since 0.6.0 */
    const val ACTION_LOCK_FREEZE = "${BuildConfig.APPLICATION_ID}.action.LOCK_FREEZE"

    fun getIntentForPackage(action: String, packageName: String) =
        Intent(action).putExtra(HailData.KEY_PACKAGE, packageName)

    fun Intent.addTag(tag: String) = putExtra(HailData.KEY_TAG, tag)

    fun getIntentForTag(action: String, tag: String) = Intent(action).addTag(tag)
}