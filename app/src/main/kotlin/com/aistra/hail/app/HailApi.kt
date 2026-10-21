package com.aistra.hail.app

import android.content.Intent

object HailApi {
    /** @since 0.5.0 */
    const val ACTION_LAUNCH = "com.aistra.hail.action.LAUNCH"

    /** @since 0.5.0 */
    const val ACTION_FREEZE = "com.aistra.hail.action.FREEZE"

    /** @since 0.5.0 */
    const val ACTION_UNFREEZE = "com.aistra.hail.action.UNFREEZE"

    /** @since 1.1.0 */
    const val ACTION_FREEZE_TAG = "com.aistra.hail.action.FREEZE_TAG"

    /** @since 1.1.0 */
    const val ACTION_UNFREEZE_TAG = "com.aistra.hail.action.UNFREEZE_TAG"

    /** @since 0.5.0 */
    const val ACTION_FREEZE_ALL = "com.aistra.hail.action.FREEZE_ALL"

    /** @since 0.5.0 */
    const val ACTION_UNFREEZE_ALL = "com.aistra.hail.action.UNFREEZE_ALL"

    /** @since 1.0.0 */
    const val ACTION_FREEZE_NON_WHITELISTED =
        "com.aistra.hail.action.FREEZE_NON_WHITELISTED"

    /** @since 1.3.0 */
    const val ACTION_FREEZE_AUTO = "com.aistra.hail.action.FREEZE_AUTO"

    /** @since 0.6.0 */
    const val ACTION_LOCK = "com.aistra.hail.action.LOCK"

    /** @since 0.6.0 */
    const val ACTION_LOCK_FREEZE = "com.aistra.hail.action.LOCK_FREEZE"

    fun getIntentForPackage(action: String, packageName: String) =
        Intent(action).putExtra(HailData.KEY_PACKAGE, packageName)

    fun Intent.addTag(tag: String) = putExtra(HailData.KEY_TAG, tag)

    fun getIntentForTag(action: String, tag: String) = Intent(action).addTag(tag)
}