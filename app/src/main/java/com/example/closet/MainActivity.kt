package com.example.closet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.closet.ui.reminder.ReminderScheduler
import com.example.closet.ui.screens.HomeScreen
import com.example.closet.ui.theme.ClosetTheme
import com.example.closet.ui.viewmodel.ClosetViewModel

class MainActivity : ComponentActivity() {

    private lateinit var closetViewModel: ClosetViewModel

    private val remindersUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (::closetViewModel.isInitialized) {
                closetViewModel.loadReminders()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        closetViewModel = ViewModelProvider(this)[ClosetViewModel::class.java]

        handleNavigationIntent(intent)

        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .crossfade(100)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            ClosetTheme {
                HomeScreen(viewModel = closetViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ReminderScheduler.ACTION_REMINDERS_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(remindersUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(remindersUpdateReceiver, filter)
        }
        if (::closetViewModel.isInitialized) {
            closetViewModel.loadReminders()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(remindersUpdateReceiver)
        } catch (_: Exception) {}
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val target = intent?.getStringExtra("TARGET_FUNCTION")
        if (!target.isNullOrBlank()) {
            if (::closetViewModel.isInitialized) {
                closetViewModel.pendingNavigationFunction = target
            }
        }
    }
}