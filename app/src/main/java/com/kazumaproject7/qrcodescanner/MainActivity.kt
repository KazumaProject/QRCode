package com.kazumaproject7.qrcodescanner

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.kazumaproject7.qrcodescanner.databinding.ActivityMainBinding
import com.kazumaproject7.qrcodescanner.other.parcelable
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.net.URI

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val navController = findNavController(R.id.fragmentHostView)
        val popMenu = PopupMenu(this,null)
        popMenu.inflate(R.menu.bottom_menu)
        val menu = popMenu.menu
        binding.bottomBar.setupWithNavController(menu,navController)
        navController.addOnDestinationChangedListener{_,destination,_ ->
            when(destination.id){
                R.id.resultFragment, R.id.historyFragment, R.id.settingsFragment, R.id.openSourceLicenseFragment ->{
                    binding.bottomBar.visibility = View.GONE
                }
            }
        }

        viewModel.isActionAndBottomBarShow.observe(this){
            if (it){
                binding.bottomBar.animate().alpha(1f).duration = 500
                binding.bottomBar.visibility = View.VISIBLE
            } else {
                binding.bottomBar.animate().alpha(0f).duration = 500
                binding.bottomBar.visibility = View.GONE
            }
        }

        // Receive image from other app
        val receivedIntent = intent
        val receivedAction = receivedIntent.action
        val receivedType = receivedIntent.type

        receivedAction?.let { action ->
            receivedType?.let { type ->
                if (action == Intent.ACTION_SEND){
                    if (type.startsWith("image/")){
                        val receiveUri = receivedIntent.parcelable<Uri>(Intent.EXTRA_STREAM)
                        receiveUri?.let { uri ->
                            viewModel.updateReceivingUri(uri)
                            viewModel.updateIsReceivingImage(true)
                        }
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}