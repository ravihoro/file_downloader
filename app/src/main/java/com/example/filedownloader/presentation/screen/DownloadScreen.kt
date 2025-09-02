import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.filedownloader.presentation.viewmodel.DownloadViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.filedownloader.presentation.components.ActiveDownloadList
import com.example.filedownloader.presentation.components.CompletedDownloadList
import android.os.Build
import android.widget.Toast
import com.example.filedownloader.presentation.components.UrlInputDialog
import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.presentation.event.DownloadEvent

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    var selectedIndex by remember { mutableStateOf(0) }

    var urlInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Scaffold (
        topBar = { TopAppBar(title = { Text("File Downloader") })},
        floatingActionButton = {
            FloatingActionButton(
                onClick = {showDialog = true},
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add new dowload")
            }
        }
    ) {  paddingValue ->
        Column (
            modifier = Modifier.padding(paddingValue).fillMaxSize()
        ){
            TabRow(selectedIndex) {
                Tab(
                    selected = selectedIndex == 0,
                    onClick = {selectedIndex = 0},
                    text = {Text("Active Downloads")}
                )

                Tab(
                    selected = selectedIndex == 1,
                    onClick = {selectedIndex = 1},
                    text = {Text("Completed Downloads")}
                )
            }

            when(selectedIndex){
                0 -> ActiveDownloadList(
                    downloads = uiState.active,
                    onPause = { task ->
                        viewModel.onEvent(
                            DownloadEvent.Pause(task.id, task.downloadedBytes, task.progress)
                        )
                    },
                    onResume = { task ->
                        viewModel.onEvent(
                            DownloadEvent.Resume(task)
                        )
                    },
                    onCancel = { task ->
                        viewModel.onEvent(DownloadEvent.Cancel(task.id))
                    }
                )
                1 -> CompletedDownloadList(
                    downloads = uiState.completed
                )
            }

        }
    }

    if(showDialog){
        UrlInputDialog(
            urlInput = urlInput,
            isLoading = uiState.isLoading,
            onUrlChange = {urlInput = it},
            onDismiss = {
                showDialog = false
                urlInput = "" },
            onAddDownload = {
                if(urlInput.trim().isNotBlank()){

                    viewModel.onEvent(DownloadEvent.Add(urlInput.trim()))

                    showDialog = false

                    urlInput = ""

                }else{
                    Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

}