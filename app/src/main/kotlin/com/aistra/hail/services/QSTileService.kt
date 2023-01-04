package com.aistra.hail.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        startActivity(
            Intent(
                when (HailData.tileAction) {
                    HailData.ACTION_FREEZE_NON_WHITELISTED -> HailApi.ACTION_FREEZE_NON_WHITELISTED
                    HailData.ACTION_LOCK_FREEZE -> HailApi.ACTION_LOCK_FREEZE
                    else -> HailApi.ACTION_FREEZE_ALL
                }
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun onTileAdded() {
        qsTile.state = Tile.STATE_ACTIVE
        updateTile()
    }

    private fun updateTile() {
        qsTile.icon = Icon.createWithResource(
            this, when (HailData.tileAction) {
                HailData.ACTION_LOCK_FREEZE -> R.drawable.ic_outline_lock
                else -> R.drawable.ic_round_frozen
            }
        )
        qsTile.label = getString(
            when (HailData.tileAction) {
                HailData.ACTION_FREEZE_NON_WHITELISTED -> R.string.action_freeze_non_whitelisted
                HailData.ACTION_LOCK_FREEZE -> R.string.action_lock_freeze
                else -> R.string.action_freeze_all
            }
        )
        qsTile.updateTile()
    }
}