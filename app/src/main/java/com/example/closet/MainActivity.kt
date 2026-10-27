package com.example.closet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.closet.ui.screens.HomeScreen
import com.example.closet.ui.theme.ClosetTheme
import com.example.closet.ui.viewmodel.ClosetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                val closetViewModel: ClosetViewModel = viewModel()
                HomeScreen(viewModel = closetViewModel)
            }
        }
    }
}