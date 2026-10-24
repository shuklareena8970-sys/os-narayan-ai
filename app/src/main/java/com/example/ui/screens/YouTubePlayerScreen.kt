package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.NarayanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlayerScreen(viewModel: NarayanViewModel) {
    BackHandler { viewModel.navigateBack() }

    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("https://www.youtube.com/watch?v=kKKM8Y-u7ds") }
    var activeVideoId by remember { mutableStateOf<String?>("kKKM8Y-u7ds") }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun extractYouTubeId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val pattern = "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/ ]{11})".toRegex()
        val match = pattern.find(trimmed)
        return match?.groupValues?.get(1)
    }

    fun playUrl(url: String) {
        val id = extractYouTubeId(url)
        if (id != null) {
            validationError = null
            activeVideoId = id
        } else {
            validationError = "Invalid YouTube URL. Please enter a valid link like https://www.youtube.com/watch?v=... or https://youtu.be/..."
            activeVideoId = null
        }
    }

    val sampleLessons = listOf(
        "Newton's Laws" to "https://www.youtube.com/watch?v=kKKM8Y-u7ds",
        "Quadratic Formula" to "https://www.youtube.com/watch?v=IlNAwlXPrG8",
        "Cell Biology" to "https://www.youtube.com/watch?v=URUJD5NEXC8"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Player", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("yt_back_button")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Enter Educational Video Link",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                if (validationError != null) validationError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("yt_url_input"),
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            isError = validationError != null,
                            supportingText = if (validationError != null) { { Text(validationError!!) } } else null,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { playUrl(urlInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("yt_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load & Play Video")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Quick Sample Lessons:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sampleLessons.forEach { (name, url) ->
                                FilterChip(
                                    selected = urlInput == url,
                                    onClick = {
                                        urlInput = url
                                        playUrl(url)
                                    },
                                    label = { Text(name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Player Card
            item {
                if (activeVideoId != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                    .background(Color.Black)
                            ) {
                                androidx.compose.runtime.key(activeVideoId) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.mediaPlaybackRequiresUserGesture = false
                                                webViewClient = WebViewClient()
                                                webChromeClient = WebChromeClient()

                                                val html = """
                                                    <!DOCTYPE html>
                                                    <html>
                                                    <head>
                                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                                                    <style>
                                                        body { margin: 0; background-color: #000; overflow: hidden; }
                                                        .iframe-container { position: relative; width: 100%; height: 100vh; }
                                                        iframe { width: 100%; height: 100%; border: none; }
                                                    </style>
                                                    </head>
                                                    <body>
                                                    <div class="iframe-container">
                                                        <iframe src="https://www.youtube-nocookie.com/embed/${activeVideoId}?rel=0&modestbranding=1" allowfullscreen></iframe>
                                                    </div>
                                                    </body>
                                                    </html>
                                                """.trimIndent()
                                                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Authorized Embed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlInput))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open in YouTube App", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else if (validationError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Guidance & Compliance Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Platform & Content Policy",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Uses YouTube official embed player.\n• Does not bypass embedding restrictions or download video streams.\n• If a video has embedding disabled by its creator, use the 'Open in YouTube App' button above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
