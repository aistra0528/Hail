package com.aistra.hail.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile.icon = Icon.createWithResource(
            this, if (HailData.tileLock) R.drawable.ic_outline_lock else R.drawable.ic_round_frozen
        )
        qsTile.label = getString(
            if (HailData.tileLock) R.string.action_lock_freeze else R.string.action_freeze_all
        )
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        startActivity(
            Intent(if (HailData.tileLock) HailApi.ACTION_LOCK_FREEZE else HailApi.ACTION_FREEZE_ALL)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}