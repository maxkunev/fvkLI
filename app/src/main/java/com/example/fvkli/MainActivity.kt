package com.example.fvkli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fvkli.data.AudioDownloader
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleWindowScreen()
        }
    }
}

@Composable
fun SimpleWindowScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Paste a link (YouTube or TikTok)") }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "fvkLI Тест загрузки",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Link") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = !isLoading && url.isNotBlank(),
                onClick = {
                    isLoading = true
                    statusText = "Downloading..."
                    scope.launch {
                        val result = AudioDownloader.downloadAudio(
                            context = context,
                            url = url,
                            onStatus = { msg -> statusText = msg },
                            onProgress = { progress -> statusText = "Downloading: ${progress.toInt()}%" }
                        )
                        statusText = result.fold(
                            onSuccess = { file ->
                                val sizeMb = String.format("%.2f", file.length() / (1024.0 * 1024.0))
                                "Ready! ${file.name} ($sizeMb МБ)"
                            },
                            onFailure = { err ->
                                "Error: ${err.localizedMessage}"
                            }
                        )
                        isLoading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                Text(
                    text = if (isLoading) "Downloading..." else "Downloading audio",
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = statusText,
                color = Color(0xFFB3B3B3),
                fontSize = 14.sp
            )
        }
    }
}