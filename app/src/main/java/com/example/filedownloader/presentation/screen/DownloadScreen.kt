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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.filedownloader.presentation.viewmodel.DownloadViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.filedownloader.presentation.components.ActiveDownloadList
import com.example.filedownloader.presentation.components.CompletedDownloadList
import android.os.Build
import androidx.compose.runtime.mutableIntStateOf
import com.example.filedownloader.presentation.components.UrlInputDialog
import com.example.filedownloader.presentation.event.DownloadEvent
import com.example.filedownloader.presentation.event.UrlInputDialogEvent
import com.example.filedownloader.presentation.viewmodel.UrlInputDialogViewModel

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    urlInputDialogViewModel: UrlInputDialogViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold (
        topBar = { TopAppBar(title = { Text("File Downloader") })},
        floatingActionButton = {
            FloatingActionButton(
                onClick = {urlInputDialogViewModel.onEvent(UrlInputDialogEvent.ShowDialog)},
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add new dowload")
            }
        }
    ) {  paddingValue ->
        Column (
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
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
                    downloads = uiState.completed,
                    onDelete = { task ->
                        viewModel.onEvent(DownloadEvent.Delete(task))
                    }
                )
            }

        }
    }

    UrlInputDialog()
}