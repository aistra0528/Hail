package com.aistra.hail.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.aistra.hail.HailApp.Companion.app
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
        if (HailData.tileAction == HailData.AUTO_FREEZE_AFTER_LOCK) {
            HailData.autoFreezeAfterLock = !HailData.autoFreezeAfterLock
            app.setAutoFreezeService(context = this)
            updateTile()
            return
        }
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
        else {
            @Suppress("DEPRECATION")
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    override fun onTileAdded() {
        updateTile()
    }

    private fun updateTile() {
        qsTile.icon = Icon.createWithResource(
            this, when (HailData.tileAction) {
                HailData.ACTION_UNFREEZE_ALL -> R.drawable.ic_round_unfrozen
                HailData.ACTION_LOCK, HailData.ACTION_LOCK_FREEZE -> R.drawable.ic_outline_lock
                else -> R.drawable.ic_round_frozen
            }
        )
        qsTile.label =
            resources.getStringArray(R.array.tile_action_entries)[HailData.TILE_ACTION_VALUES.indexOf(HailData.tileAction)]
        qsTile.state =
            if (HailData.tileAction != HailData.AUTO_FREEZE_AFTER_LOCK || HailData.autoFreezeAfterLock) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}