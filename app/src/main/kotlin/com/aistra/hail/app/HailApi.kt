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

    /** @since 0.5.0 */
    const val ACTION_FREEZE_ALL = "${BuildConfig.APPLICATION_ID}.action.FREEZE_ALL"

    /** @since 0.5.0 */
    const val ACTION_UNFREEZE_ALL = "${BuildConfig.APPLICATION_ID}.action.UNFREEZE_ALL"

    /** @since 0.6.0 */
    const val ACTION_LOCK = "${BuildConfig.APPLICATION_ID}.action.LOCK"

    /** @since 0.6.0 */
    const val ACTION_LOCK_FREEZE = "${BuildConfig.APPLICATION_ID}.action.LOCK_FREEZE"

    fun getIntentForPackage(action: String, packageName: String) =
        Intent(action).putExtra(HailData.KEY_PACKAGE, packageName)
}