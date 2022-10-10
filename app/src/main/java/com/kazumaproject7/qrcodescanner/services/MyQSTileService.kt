package com.kazumaproject7.qrcodescanner.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.kazumaproject7.qrcodescanner.MainActivity
import com.kazumaproject7.qrcodescanner.R

@RequiresApi(Build.VERSION_CODES.N)
class MyQSTileService : TileService(){

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.app_name)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        Toast.makeText(this,"",Toast.LENGTH_LONG).show()
        val intent = Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }

}