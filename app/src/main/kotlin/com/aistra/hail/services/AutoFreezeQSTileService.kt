package com.aistra.hail.services

import android.annotation.SuppressLint
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HTarget
import com.aistra.hail.work.HWork

@RequiresApi(Build.VERSION_CODES.N)
class AutoFreezeQSTileService : TileService() {
    private var updateTileThread: Thread? = null
    private var tileState = AutoFreezeQSTileState.ENABLED
    private var nextClickShouldEnable = false

    override fun onStartListening() {
        super.onStartListening()

        tileState = AutoFreezeQSTileState.tileStateFromDisabledUntil(HailData.autoFreezeDisabledUntil)
        if (tileState != AutoFreezeQSTileState.ENABLED) {
            // UX: assuming that the user wants to re-enable if tapping on the tile while disabled
            nextClickShouldEnable = true
        }

        updateTileThread = Thread {
            try {
                while (true) {
                    updateTile()
                    // This shows the countdown nicely, but has the problem that it restarts the tile's label every second making the scrolling to restart. TODO solve some other way? Is it possible to avoid restarting of scrolling?
                    Thread.sleep(1000)
                }
            } catch (_: InterruptedException) {}
        }
        updateTileThread?.start()

        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        updateTileThread?.interrupt()
        updateTileThread = null
    }

    override fun onClick() {
        super.onClick()

        if (nextClickShouldEnable) {
            tileState = AutoFreezeQSTileState.ENABLED
            nextClickShouldEnable = false
        } else {
            tileState = tileState.next
        }

        HailData.autoFreezeDisabledUntil = tileState.disableUntil

        when (tileState) {
            AutoFreezeQSTileState.ENABLED -> {
                HailData.autoFreezeAfterLock = true
                HWork.cancelWork(HailApi.DISABLE_AUTO_FREEZE_SERVICE_TEMPORARILY)
                app.setAutoFreezeService()
            }
            AutoFreezeQSTileState.DISABLED -> {
                HailData.autoFreezeAfterLock = false
                app.stopAutoFreezeService()
            }
            else -> {
                HWork.disableAutoFreezeServiceFor(tileState.millis)
            }
        }
        updateTile()
    }

    override fun onTileAdded() {
        updateTile()
    }

    @SuppressLint("NewApi")
    private fun updateTile() {
        if (!HailData.autoFreezeAfterLock) {
            qsTile.state = Tile.STATE_INACTIVE
            if (HTarget.Q) qsTile.subtitle = getString(R.string.auto_freeze_tile_off)
            else qsTile.label = getString(R.string.auto_freeze_tile_off)
        } else if (HailData.autoFreezeDisabledUntil > System.currentTimeMillis()) {
            qsTile.state = Tile.STATE_INACTIVE
            if (HTarget.Q) qsTile.subtitle = disabledFor(HailData.autoFreezeDisabledUntil)
            else qsTile.label = disabledFor(HailData.autoFreezeDisabledUntil)
        } else {
            qsTile.state = Tile.STATE_ACTIVE
            if (HTarget.Q) qsTile.subtitle = getString(R.string.auto_freeze_tile_on)
            else qsTile.label = getString(R.string.auto_freeze_tile_on)
        }

        qsTile.updateTile()
    }

    private fun disabledFor(epochMilliseconds: Long): String {
        val timeout = epochMilliseconds - System.currentTimeMillis()
        return DateUtils.formatElapsedTime(timeout / 1000)
    }
}

enum class AutoFreezeQSTileState(val millis: Long) {
    ENABLED(0),
    DISABLED_FOR_ONE_MINUTE(60 * 1000),
    DISABLED_FOR_FIVE_MINUTES(5 * 60 * 1000),
    DISABLED_FOR_TEN_MINUTES(10 * 60 * 1000),
    DISABLED_FOR_THIRTY_MINUTES(30 * 60 * 1000),
    DISABLED_FOR_ONE_HOUR(60 * 60 * 1000),
    DISABLED_FOR_TWO_HOURS(120 * 60 * 1000),
    DISABLED(Long.MAX_VALUE / 2); // Not Long.MAX_VALUE so that adding System.currentTimeMillis while calculating 'disableUntil' doesn't make it turn around

    val disableUntil: Long get() = System.currentTimeMillis() + millis

    val next: AutoFreezeQSTileState
        get() {
            val values = enumValues<AutoFreezeQSTileState>()
            val nextOrdinal = (ordinal + 1) % values.size
            return values[nextOrdinal]
        }

    companion object {
        fun tileStateFromDisabledUntil(disabledUntil: Long): AutoFreezeQSTileState {
            var tileState = DISABLED

            for (value in enumValues<AutoFreezeQSTileState>()) {
                if (disabledUntil - System.currentTimeMillis() <= value.millis) {
                    tileState = value
                    break
                }
            }

            return tileState
        }
    }
}
