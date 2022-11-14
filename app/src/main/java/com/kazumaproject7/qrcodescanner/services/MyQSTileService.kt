package com.kazumaproject7.qrcodescanner.services

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.kazumaproject7.qrcodescanner.MainActivity
import com.kazumaproject7.qrcodescanner.R

class MyQSTileService : TileService(){

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Scan QR Code"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }

}