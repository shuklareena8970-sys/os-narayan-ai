package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VideoRecord
import com.example.ui.NarayanViewModel

@Composable
fun AdminAppControlsSection(viewModel: NarayanViewModel) {
    val announcement by viewModel.announcement.collectAsState()
    val subjects by viewModel.customSubjects.collectAsState()
    val currentAdminId by viewModel.superAdminId.collectAsState()
    val appMotto by viewModel.appMotto.collectAsState()
    val publishedLectures by viewModel.publishedLectures.collectAsState()

    // Credential Change State
    var oldPassInput by remember { mutableStateOf("") }
    var newIdInput by remember { mutableStateOf(currentAdminId) }
    var newPassInput by remember { mutableStateOf("") }
    var showOldPass by remember { mutableStateOf(false) }
    var showNewPass by remember { mutableStateOf(false) }
    var credStatusMessage by remember { mutableStateOf<String?>(null) }
    var credErrorMessage by remember { mutableStateOf<String?>(null) }

    // Announcement State
    var annEnabled by remember(announcement) { mutableStateOf(announcement.isEnabled) }
    var annTitle by remember(announcement) { mutableStateOf(announcement.title) }
    var annMessage by remember(announcement) { mutableStateOf(announcement.message) }
    var annSavedNotice by remember { mutableStateOf(false) }

    // Subject State
    var newSubjectInput by remember { mutableStateOf("") }

    // Motto State
    var mottoInput by remember(appMotto) { mutableStateOf(appMotto) }
    var mottoSavedNotice by remember { mutableStateOf(false) }

    // Edit Lecture State
    var videoToEdit by remember { mutableStateOf<VideoRecord?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editSubject by remember { mutableStateOf("") }
    var editChapter by remember { mutableStateOf("") }
    var editLanguage by remember { mutableStateOf("") }
    var editUri by remember { mutableStateOf("") }
    var editKeyPoints by remember { mutableStateOf("") }
    var editDefinitions by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Super Admin Security & Password
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Super Admin ID & Password", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current Active Admin ID: $currentAdminId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (credStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(credStatusMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (credErrorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(credErrorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = oldPassInput,
                    onValueChange = { oldPassInput = it },
                    label = { Text("Current Password *") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_old_pass_input"),
                    visualTransformation = if (showOldPass) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showOldPass = !showOldPass }) {
                            Icon(if (showOldPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newIdInput,
                    onValueChange = { newIdInput = it },
                    label = { Text("New Super Admin ID *") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_new_id_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassInput,
                    onValueChange = { newPassInput = it },
                    label = { Text("New Super Admin Password *") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_new_pass_input"),
                    visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showNewPass = !showNewPass }) {
                            Icon(if (showNewPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        credStatusMessage = null
                        credErrorMessage = null
                        val result = viewModel.changeSuperAdminCredentials(oldPassInput, newIdInput, newPassInput)
                        result.onSuccess {
                            credStatusMessage = "Super Admin ID and Password updated successfully!"
                            oldPassInput = ""
                            newPassInput = ""
                        }.onFailure {
                            credErrorMessage = it.message ?: "Failed to update credentials"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_admin_creds_btn")
                ) {
                    Text("Save New Admin Credentials")
                }
            }
        }

        // Section 2: Announcement Notice Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Notice Banner", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = annEnabled,
                        onCheckedChange = { annEnabled = it }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Broadcast important announcements, schedule alerts, or exam notices across the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = annTitle,
                    onValueChange = {
                        annTitle = it
                        annSavedNotice = false
                    },
                    label = { Text("Banner Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = annMessage,
                    onValueChange = {
                        annMessage = it
                        annSavedNotice = false
                    },
                    label = { Text("Banner Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.updateAnnouncement(annEnabled, annTitle, annMessage)
                        annSavedNotice = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (annSavedNotice) "✓ Banner Saved" else "Update Notice Banner")
                }
            }
        }

        // Section 3: App Motto & Tagline
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Tagline & Welcome Motto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mottoInput,
                    onValueChange = {
                        mottoInput = it
                        mottoSavedNotice = false
                    },
                    label = { Text("Welcome Tagline") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.updateAppMotto(mottoInput)
                        mottoSavedNotice = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (mottoSavedNotice) "✓ Motto Saved" else "Update Tagline")
                }
            }
        }

        // Section 4: Curriculum Subjects
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Curriculum Subjects", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubjectInput,
                        onValueChange = { newSubjectInput = it },
                        placeholder = { Text("Add subject e.g. Economics") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newSubjectInput.isNotBlank()) {
                                viewModel.addCustomSubject(newSubjectInput)
                                newSubjectInput = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Active Subjects in App:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subjects.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Section 5: Direct Lecture Content Editing
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit Existing Public Lectures", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Directly update title, video URL, formulas, or key points of published lectures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                publishedLectures.forEach { lecture ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lecture.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("${lecture.subject} • ${lecture.chapter}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(
                            onClick = {
                                videoToEdit = lecture
                                editTitle = lecture.title
                                editDesc = lecture.description
                                editSubject = lecture.subject
                                editChapter = lecture.chapter
                                editLanguage = lecture.language
                                editUri = lecture.videoUri
                                editKeyPoints = lecture.keyPoints
                                editDefinitions = lecture.definitions
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Lecture", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Edit Lecture Dialog
    videoToEdit?.let { lecture ->
        AlertDialog(
            onDismissRequest = { videoToEdit = null },
            title = { Text("Edit Lecture Content") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUri,
                        onValueChange = { editUri = it },
                        label = { Text("Video URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSubject,
                        onValueChange = { editSubject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editChapter,
                        onValueChange = { editChapter = it },
                        label = { Text("Chapter") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editKeyPoints,
                        onValueChange = { editKeyPoints = it },
                        label = { Text("Key Points") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDefinitions,
                        onValueChange = { editDefinitions = it },
                        label = { Text("Formulas / Definitions") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminEditVideo(
                            video = lecture,
                            title = editTitle,
                            description = editDesc,
                            subject = editSubject,
                            chapter = editChapter,
                            language = editLanguage,
                            videoUri = editUri,
                            keyPoints = editKeyPoints,
                            definitions = editDefinitions
                        )
                        videoToEdit = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
