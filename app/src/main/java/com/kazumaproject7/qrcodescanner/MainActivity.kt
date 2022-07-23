package com.kazumaproject7.qrcodescanner

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.kazumaproject7.qrcodescanner.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

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
                else ->{
                    binding.bottomBar.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}