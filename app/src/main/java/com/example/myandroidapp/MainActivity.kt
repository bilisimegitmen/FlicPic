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
import com.example.myandroidapp.ui.theme.ColorDark0
import com.example.myandroidapp.ui.theme.MyAndroidAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyAndroidAppTheme {
                val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val isGranted = results.values.any { it }
                    viewModel.onPermissionResult(isGranted)
                }

                // Initial permission check on launch
                LaunchedEffect(Unit) {
                    val hasPermission = requiredPermissions.any { perm ->
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            perm
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                    viewModel.onPermissionResult(hasPermission)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = ColorDark0
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
