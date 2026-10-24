package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VideoRecord
import com.example.ui.NarayanViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishVideoScreen(viewModel: NarayanViewModel) {
    BackHandler { viewModel.navigateBack() }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    val userPrivateVideos by viewModel.userPrivateVideos.collectAsState()
    val allUserVideos by viewModel.adminAllSubmissions.collectAsState()
    val mySubmissions = allUserVideos.filter { it.ownerId == "current_user" && it.status != VideoRecord.STATUS_PRIVATE }

    var showAddPrivateDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoRecord?>(null) }

    // Public request form states
    var pubTitle by remember { mutableStateOf("") }
    var pubDesc by remember { mutableStateOf("") }
    var pubSubject by remember { mutableStateOf("Physics") }
    var pubChapter by remember { mutableStateOf("Chapter 1") }
    var pubLang by remember { mutableStateOf("English") }
    var pubUrl by remember { mutableStateOf("") }
    var pubKeyPoints by remember { mutableStateOf("") }
    var pubDefinitions by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Publish & Study Videos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("publish_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Private Videos") },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Request Public Publish") },
                    icon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: Private Videos
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Private Videos are strictly for your personal study. They are never shown in the public Lectures library.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { showAddPrivateDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save New Private Study Video")
                        }
                    }

                    if (userPrivateVideos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No private videos saved",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Save personal revision video links or files above.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(userPrivateVideos) { video ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${video.subject} • ${video.videoUri}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(onClick = { videoToDelete = video }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: Request Public Publishing
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Public Submission & Approval Workflow",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Submitting an educational video assigns it status 'Pending Approval'. Videos never publish automatically. A Super Admin must review and approve it before it appears in public Lectures.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Form Fields
                    item {
                        OutlinedTextField(
                            value = pubTitle,
                            onValueChange = { pubTitle = it },
                            label = { Text("Lecture Title *") },
                            placeholder = { Text("e.g. Thermodynamics Laws") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pubSubject,
                                onValueChange = { pubSubject = it },
                                label = { Text("Subject *") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pubChapter,
                                onValueChange = { pubChapter = it },
                                label = { Text("Chapter") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pubLang,
                                onValueChange = { pubLang = it },
                                label = { Text("Language") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pubUrl,
                                onValueChange = { pubUrl = it },
                                label = { Text("YouTube URL *") },
                                placeholder = { Text("https://www.youtube.com/...") },
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = pubDesc,
                            onValueChange = { pubDesc = it },
                            label = { Text("Detailed Description") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = pubKeyPoints,
                            onValueChange = { pubKeyPoints = it },
                            label = { Text("Key Learning Points") },
                            placeholder = { Text("• Point 1\n• Point 2") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = pubDefinitions,
                            onValueChange = { pubDefinitions = it },
                            label = { Text("Formulas & Definitions") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (formError != null) {
                        item {
                            Text(
                                text = formError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (pubTitle.isBlank() || pubUrl.isBlank()) {
                                    formError = "Title and YouTube URL are required."
                                } else {
                                    viewModel.requestPublicPublishing(
                                        title = pubTitle,
                                        description = pubDesc,
                                        subject = pubSubject,
                                        chapter = pubChapter,
                                        language = pubLang,
                                        videoUri = pubUrl,
                                        keyPoints = pubKeyPoints,
                                        definitions = pubDefinitions
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Video submitted for review! Status: Pending Approval")
                                    }
                                    pubTitle = ""
                                    pubDesc = ""
                                    pubUrl = ""
                                    pubKeyPoints = ""
                                    pubDefinitions = ""
                                    formError = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit for Public Approval")
                        }
                    }

                    // My Submissions status list
                    if (mySubmissions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "My Submission History (${mySubmissions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(mySubmissions) { sub ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sub.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        StatusBadge(status = sub.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${sub.subject} (${sub.chapter}) • ${sub.language}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (sub.status == VideoRecord.STATUS_REJECTED && sub.rejectionReason.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Rejection Reason: ${sub.rejectionReason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Add Private Video Dialog
    if (showAddPrivateDialog) {
        var privTitle by remember { mutableStateOf("") }
        var privDesc by remember { mutableStateOf("") }
        var privSub by remember { mutableStateOf("General") }
        var privUri by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPrivateDialog = false },
            title = { Text("Save Private Study Video", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = privTitle,
                        onValueChange = { privTitle = it },
                        label = { Text("Video Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privSub,
                        onValueChange = { privSub = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privUri,
                        onValueChange = { privUri = it },
                        label = { Text("Video Link / Local Path *") },
                        placeholder = { Text("https://www.youtube.com/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = privDesc,
                        onValueChange = { privDesc = it },
                        label = { Text("Notes / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (privTitle.isNotBlank() && privUri.isNotBlank()) {
                            viewModel.addPrivateVideo(privTitle, privDesc, privSub, privUri)
                            showAddPrivateDialog = false
                        }
                    }
                ) {
                    Text("Save Private")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPrivateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    videoToDelete?.let { vid ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video?") },
            text = { Text("Are you sure you want to delete '${vid.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(vid)
                        videoToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        VideoRecord.STATUS_PUBLISHED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        VideoRecord.STATUS_APPROVED -> Color(0xFFE8DEF8) to Color(0xFF1D192B)
        VideoRecord.STATUS_PENDING_APPROVAL -> Color(0xFFFFF3CD) to Color(0xFF664D03)
        VideoRecord.STATUS_REJECTED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        VideoRecord.STATUS_UNPUBLISHED -> Color(0xFFE2E3E5) to Color(0xFF383D41)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
