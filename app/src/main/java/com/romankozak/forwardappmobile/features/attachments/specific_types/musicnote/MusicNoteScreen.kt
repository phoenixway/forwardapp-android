package com.romankozak.forwardappmobile.features.attachments.specific_types.musicnote

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicNoteScreen(
    navController: NavController,
    startEdit: Boolean = false,
    viewModel: MusicNoteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isEditMode by remember { mutableStateOf(startEdit) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.name.ifBlank { "Ноти" }) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isEditMode) viewModel.save()
                            navController.popBackStack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isEditMode) {
                        viewModel.save()
                    }
                    isEditMode = !isEditMode
                },
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Filled.Visibility else Icons.Filled.Edit,
                    contentDescription = if (isEditMode) "View" else "Edit",
                )
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (isEditMode) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    singleLine = true,
                    label = { Text("Назва") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    label = { Text("Ноти") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        } else {
            val xml = remember(uiState.name, uiState.content) { MusicNoteToMusicXmlConverter.convert(uiState.content, uiState.name) }
            MusicXmlViewer(
                musicXml = xml,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MusicXmlViewer(
    musicXml: String,
    modifier: Modifier = Modifier,
) {
    fun renderXml(webView: WebView, xml: String) {
        val escaped = JSONObject.quote(xml)
        webView.evaluateJavascript("window.renderMusicXml($escaped);", null)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            if (view != null) {
                                renderXml(view, musicXml)
                            }
                        }
                    }
                loadUrl("file:///android_asset/musicxml_viewer.html")
            }
        },
        update = { webView ->
            renderXml(webView, musicXml)
        },
    )
}
