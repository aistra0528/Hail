package com.aistra.hail.services

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HTarget

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(
            when (HailData.tileAction) {
                HailData.ACTION_FREEZE_ALL -> HailApi.ACTION_FREEZE_ALL
                HailData.ACTION_FREEZE_NON_WHITELISTED -> HailApi.ACTION_FREEZE_NON_WHITELISTED
                HailData.ACTION_LOCK -> HailApi.ACTION_LOCK
                HailData.ACTION_LOCK_FREEZE -> HailApi.ACTION_LOCK_FREEZE
                else -> HailApi.ACTION_UNFREEZE_ALL
            }
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (HTarget.U) startActivityAndCollapse(
            PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
        )
        else startActivityAndCollapse(intent)
    }

    override fun onTileAdded() {
        qsTile.state = Tile.STATE_ACTIVE
        updateTile()
    }

    private fun updateTile() {
        qsTile.icon = Icon.createWithResource(
            this, when (HailData.tileAction) {
                HailData.ACTION_FREEZE_ALL, HailData.ACTION_FREEZE_NON_WHITELISTED -> R.drawable.ic_round_frozen
                HailData.ACTION_LOCK, HailData.ACTION_LOCK_FREEZE -> R.drawable.ic_outline_lock
                else -> R.drawable.ic_round_unfrozen
            }
        )
        qsTile.label = getString(
            when (HailData.tileAction) {
                HailData.ACTION_FREEZE_ALL -> R.string.action_freeze_all
                HailData.ACTION_FREEZE_NON_WHITELISTED -> R.string.action_freeze_non_whitelisted
                HailData.ACTION_LOCK -> R.string.action_lock
                HailData.ACTION_LOCK_FREEZE -> R.string.action_lock_freeze
                else -> R.string.action_unfreeze_all
            }
        )
        qsTile.updateTile()
    }
}