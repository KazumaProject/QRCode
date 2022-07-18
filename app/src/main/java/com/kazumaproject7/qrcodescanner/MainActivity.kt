package com.kazumaproject7.qrcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.kazumaproject7.qrcodescanner.databinding.ActivityMainBinding
import com.kazumaproject7.qrcodescanner.other.Constants
import com.kazumaproject7.qrcodescanner.ui.capture.CaptureFragment
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object{
        private const val REQUEST_CODE_PERMISSIONS = 77
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        if (!allPermissionsGranted()){
            ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            //startCaptureFragment()
        }

        val navController = findNavController(R.id.fragmentHostView)
        val popMenu = PopupMenu(this,null)
        popMenu.inflate(R.menu.bottom_menu)
        val menu = popMenu.menu
        binding.bottomBar.setupWithNavController(menu,navController)

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (!allPermissionsGranted()) {
                this.finish()
            } else {
                //startCaptureFragment()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

//    private fun startCaptureFragment(){
//        val cameraCaptureFragment = CaptureFragment()
//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.add(binding.fragmentHostView.id,cameraCaptureFragment,Constants.CAPTURE_FRAGMENT)
//        transaction.commit()
//    }

}