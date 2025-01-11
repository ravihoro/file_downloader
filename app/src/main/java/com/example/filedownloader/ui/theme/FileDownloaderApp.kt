package com.example.filedownloader.ui.theme

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext


@Composable
fun FileDownloaderApp() {

    val context = LocalContext.current

    val storagePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if(!isGranted){
                println("Storage permission denied")
            }
        }
    )

    LaunchedEffect(Unit) {

    }

}