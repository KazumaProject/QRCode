package com.kazumaproject7.qrcodescanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kazumaproject7.qrcodescanner.databinding.ActivityMainBinding
import com.kazumaproject7.qrcodescanner.other.parcelable
import com.kazumaproject7.qrcodescanner.ui.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.updateReceivingUri(uri)
                                viewModel.updateIsReceivingImage(true)
                            }
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