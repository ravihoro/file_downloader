package com.example.filedownloader

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DownloadItem(task: DownloadTask, downloadManager: DownloadManager) {

    var isExpanded by remember { mutableStateOf(false) }

    task.let {
        val progress = it.progress
        val status = it.status

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .height(80.dp)
                    .width(80.dp)

            ) {
                Box(
                    modifier = Modifier
                        .height(70.dp)
                        .width(70.dp)
                        .background(Color.Blue)
                ) {
                    Text(
                        it.fileName.split(".").last(),
                        style = TextStyle(color = Color.White, fontSize = 18.sp),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(30.dp)
                        .background(Color.Gray, shape = CircleShape)
                        .align(Alignment.BottomEnd)
                        .clickable {
                            if (status == DownloadStatus.ACTIVE) {
                                downloadManager.pauseDownload(it.id)
                            } else if (status == DownloadStatus.PAUSED) {
                                downloadManager.resumeDownload(it);
                            }
                        },

                    ) {
                    Icon(
                        imageVector = if (status == DownloadStatus.ACTIVE) Icons.Filled.Pause else Icons.Default.PlayArrow,
                        contentDescription = "",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)

            ) {
                Text(it.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)

                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    //Log.d("DownloadManager", "UIIIIII $status ${it.speed != ""}")
                    if (status == DownloadStatus.ACTIVE && it.speed != "") {
                        Row {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download Indicator"
                            )
                            Text(it.speed)
                        }

                    } else if (status == DownloadStatus.PAUSED) {
                        Text("$status")
                    }

                    if(task.isLoading){
                        Text("Requesting Info...")
                    }else if(task.message != ""){
                        Text(task.message, style = TextStyle(color = Color.Red))
                    }
                }



                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "${formatBytes(it.downloadedBytes)} / ${formatBytes(it.totalBytes)}",
                        style = TextStyle(fontSize = 14.sp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${String.format("%.0f", progress)} %",
                            style = TextStyle(fontSize = 14.sp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier

                                .width(40.dp)
                                .height(10.dp),
                            color = Color.Blue,
                            trackColor = Color.Gray,
                        )
                    }


                }
            }

            IconButton(
                onClick = { isExpanded = !isExpanded },
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More Options",
                    modifier = Modifier.size(35.dp)
                )
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },

                ) {
                DropdownMenuItem(
                    onClick = { isExpanded = false },
                    text = { Text("Text 1") },
                )
            }

        }
    }
}