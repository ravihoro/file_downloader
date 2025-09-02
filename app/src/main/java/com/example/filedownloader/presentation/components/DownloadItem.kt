package com.example.filedownloader.presentation.components

import android.app.DownloadManager
import android.content.Intent
import android.os.Build
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.utils.formatBytes


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DownloadItem(task: DownloadTask, onPause: () -> Unit, onResume: () -> Unit, onCancel: () -> Unit, onDelete: () -> Unit) {

    val context = LocalContext.current;

    var isExpanded by remember { mutableStateOf(false) }

    val progress = task.progress
    val status = task.status

    val textStyle = TextStyle(fontSize = 12.sp)

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {

        Box(modifier = Modifier.padding(10.dp).height(80.dp).width(80.dp)){
            Box(modifier = Modifier
                .size(70.dp)
                .background(Color.Blue), contentAlignment = Alignment.Center,){
                Text(text = task.fileName.substringAfterLast("."), color = Color.White)
            }

            if(status != DownloadStatus.COMPLETED){
                Box(
                    modifier = Modifier.height(30.dp).width(30.dp).background(Color.Gray, shape = CircleShape)
                        .align(Alignment.BottomEnd)
                        .clickable {
                            when(status) {
                                DownloadStatus.ACTIVE -> onPause()
                                DownloadStatus.PAUSED -> onResume()
                                else -> {}
                            }
                        }
                ){
                    Icon(
                        imageVector = if(status ==  DownloadStatus.ACTIVE) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Pause/Resume",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                task.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ){

                if(status == DownloadStatus.ACTIVE && task.speed.isNotEmpty()){
                    Row (
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Downloading", Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(task.speed, fontSize = 14.sp)
                    }
                } else if(status == DownloadStatus.PAUSED){
                    Text("Paused", fontSize = 14.sp)
                } else if(task.message.isNotEmpty()){
                    Text(task.message, color = Color.Red)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}", style = textStyle,)

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${progress.toInt()}%", style = textStyle)
                    Spacer(Modifier.width(5.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.width(60.dp).height(8.dp),
                        color = Color.Blue,
                        gapSize = (-15).dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = Color.Gray,
                        drawStopIndicator = {}
                    )
                }

            }

        }

        Spacer(Modifier.width(8.dp))

        Box{
            IconButton(
                onClick = {
                    isExpanded = !isExpanded
                }
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false}
                ) {
                    if(status == DownloadStatus.ACTIVE || status == DownloadStatus.PAUSED){
                        DropdownMenuItem(text = {Text("Cancel")},
                            onClick = {
                                onCancel()
                                isExpanded = false
                            })
                    }else if(status == DownloadStatus.COMPLETED ){

                        DropdownMenuItem(text = {Text("Show in Downloads")},
                            onClick = {



                                isExpanded = false

                                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)

                            })

                        DropdownMenuItem(text = {Text("Delete")},
                            onClick = {
                                onDelete()
                                isExpanded = false
                            })
                    }

                }
            }
        }

    }

}