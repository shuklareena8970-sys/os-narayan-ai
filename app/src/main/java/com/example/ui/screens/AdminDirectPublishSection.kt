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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.ui.NarayanViewModel

@Composable
fun AdminDirectPublishSection(viewModel: NarayanViewModel) {
    val subjects by viewModel.customSubjects.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "Physics") }
    var chapter by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("English") }
    var videoUri by remember { mutableStateOf("") }
    var keyPoints by remember { mutableStateOf("") }
    var definitions by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Publish,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Super Admin Direct Publishing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Publish educational video lectures directly to the public library without approval delays.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (successMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Lecture Title *") },
            placeholder = { Text("e.g. Electromagnetic Waves & Maxwell's Equations") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_lecture_title"),
            singleLine = true
        )

        OutlinedTextField(
            value = videoUri,
            onValueChange = { videoUri = it },
            label = { Text("YouTube Video URL *") },
            placeholder = { Text("https://www.youtube.com/watch?v=...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_lecture_url"),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = selectedSubject,
                onValueChange = { selectedSubject = it },
                label = { Text("Subject *") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_lecture_subject"),
                singleLine = true
            )
            OutlinedTextField(
                value = chapter,
                onValueChange = { chapter = it },
                label = { Text("Chapter") },
                placeholder = { Text("e.g. Chapter 4") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_lecture_chapter"),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = language,
            onValueChange = { language = it },
            label = { Text("Instruction Language") },
            placeholder = { Text("English / Hindi / Bilingual") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Comprehensive Description *") },
            placeholder = { Text("Explain core concepts taught in this video lecture...") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = keyPoints,
            onValueChange = { keyPoints = it },
            label = { Text("Key Learning Points (Bullet format)") },
            placeholder = { Text("• Point 1: ...\n• Point 2: ...") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = definitions,
            onValueChange = { definitions = it },
            label = { Text("Formulas & Definitions") },
            placeholder = { Text("e.g. E = mc² | Frequency = c / λ") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (title.isBlank() || videoUri.isBlank() || description.isBlank()) {
                    errorMessage = "Please enter Title, YouTube URL, and Description."
                    successMessage = null
                    return@Button
                }
                errorMessage = null
                viewModel.adminDirectPublishVideo(
                    title = title,
                    description = description,
                    subject = selectedSubject,
                    chapter = chapter.ifBlank { "Chapter 1" },
                    language = language.ifBlank { "English" },
                    videoUri = videoUri,
                    keyPoints = keyPoints,
                    definitions = definitions
                )
                successMessage = "Lecture '$title' published directly to Public Lectures!"
                title = ""
                description = ""
                videoUri = ""
                chapter = ""
                keyPoints = ""
                definitions = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_direct_publish_btn"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Publish, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish Lecture Immediately", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
