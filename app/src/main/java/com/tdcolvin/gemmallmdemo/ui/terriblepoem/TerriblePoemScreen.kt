package com.tdcolvin.gemmallmdemo.ui.terriblepoem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcolvin.gemmallmdemo.R
import com.tdcolvin.gemmallmdemo.ui.reactiongesture.ReactionGestureScreen
import com.tdcolvin.gemmallmdemo.ui.takephoto.TakePhotoScreen

@Composable
fun TerriblePoemScreen(
    modifier: Modifier = Modifier,
    initialPoemSubject: String? = null,
    viewModel: TerriblePoemViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        if (!uiState.loaded) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        else {
            TerriblePoemContent(
                initialPoemSubject = initialPoemSubject,
                poemTitle = uiState.poemTitle,
                poemVerse = uiState.poemVerse,
                poemComplete = uiState.poemComplete,
                reactions = uiState.reactions,
                loadingError = uiState.loadingError,
                generateTerriblePoem = viewModel::generateTerriblePoem,
                addReaction = viewModel::addReaction
            )
        }
    }
}

@Composable
fun TerriblePoemContent(
    initialPoemSubject: String? = null,
    poemTitle: String?,
    poemVerse: String?,
    poemComplete: Boolean,
    reactions: String,
    loadingError: Throwable?,
    generateTerriblePoem: (String) -> Unit,
    addReaction: (String) -> Unit,
) {
    var poemSubject by remember { mutableStateOf(initialPoemSubject ?: "") }

    var showTakePhotoDialog by remember { mutableStateOf(false) }
    var showReactionGestureDialog by remember { mutableStateOf(false) }

    // If we have a subject passed in, generate the poem immediately
    LaunchedEffect(initialPoemSubject) {
        if (initialPoemSubject != null) {
            generateTerriblePoem(initialPoemSubject)
        }
    }

    Column {
        Text("Write a terrible poem about:")
        Row {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = poemSubject,
                onValueChange = { poemSubject = it }
            )
            Button(onClick = { showTakePhotoDialog = true }) {
                Icon(painter = painterResource(id = R.drawable.baseline_camera_alt_24), contentDescription = "Take a photo")
            }
        }
        Button(
            onClick = { generateTerriblePoem(poemSubject) },
            enabled = poemComplete && loadingError == null
        ) {
            Text("Generate Terrible Poetry")
        }

        if (loadingError != null) {
            Text("Error loading model: ${loadingError.message ?: "[Unknown]"}")
        }

        Poem(
            modifier = Modifier.padding(top = 20.dp),
            title = if (poemTitle.isNullOrBlank()) "" else """"$poemTitle"""",
            verses = poemVerse ?: "",
            complete = poemComplete
        )

//        Text(reactions)
//
//        Button(onClick = { showReactionGestureDialog = true }) {
//            Text("React")
//        }
    }

    if (showTakePhotoDialog) {
        TakePhotoDialog(
            onDismiss = { showTakePhotoDialog = false },
            onSetSubject = { subject ->
                showTakePhotoDialog = false
                poemSubject = subject
                generateTerriblePoem(subject)
            }
        )
    }

    if (showReactionGestureDialog) {
        ReactionGestureDialog(
            onDismiss = { showReactionGestureDialog = false },
            onAddReaction = addReaction
        )
    }
}

@Composable
fun TakePhotoDialog(
    onDismiss: () -> Unit,
    onSetSubject: (String) -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            TakePhotoScreen(modifier = Modifier.fillMaxSize(), setPhotoSubject = onSetSubject)
        }
    }
}

@Composable
fun ReactionGestureDialog(
    onDismiss: () -> Unit,
    onAddReaction: (String) -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            ReactionGestureScreen(
                modifier = Modifier.fillMaxSize(),
                setGesture = { gesture ->
                    gesture?.let { onAddReaction(it) }
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun Poem(
    modifier: Modifier = Modifier,
    title: String,
    verses: String,
    complete: Boolean
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (!complete) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 10.dp).size(20.dp),
                    strokeWidth = 3.dp
                )
            }
        }
        Text(verses)
    }
}