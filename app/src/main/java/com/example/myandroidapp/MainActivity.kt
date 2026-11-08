package com.example.myandroidapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.myandroidapp.ui.ImageOrganizerScreen
import com.example.myandroidapp.ui.MainViewModel
import com.example.myandroidapp.ui.theme.ColorCanvas
import com.example.myandroidapp.ui.theme.MyAndroidAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyAndroidAppTheme {
                val requiredPermissions = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    else -> arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        results[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                        results[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
                    } else {
                        results.values.any { it }
                    }
                    viewModel.onPermissionResult(isGranted)
                }

                // Initial permission check on launch
                LaunchedEffect(Unit) {
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        requiredPermissions.any { perm ->
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                perm
                            ) == PackageManager.PERMISSION_GRANTED
                        }
                    }
                    viewModel.onPermissionResult(hasPermission)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = ColorCanvas
                ) {
                    ImageOrganizerScreen(
                        viewModel = viewModel,
                        onRequestPermission = {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    )
                }
            }
        }
    }
}
