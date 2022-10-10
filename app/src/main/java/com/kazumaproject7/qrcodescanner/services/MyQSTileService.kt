package com.kazumaproject7.qrcodescanner.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.kazumaproject7.qrcodescanner.MainActivity
import com.kazumaproject7.qrcodescanner.R

@RequiresApi(Build.VERSION_CODES.N)
class MyQSTileService : TileService(){

    // Called when the user adds your tile.
    override fun onTileAdded() {
        super.onTileAdded()

    }
    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.app_name)
            updateTile()
        }
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()

    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        val intent = Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }
    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()

    }

}