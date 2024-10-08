package com.tdcolvin.gemmallmdemo.ui.screens

import android.app.Application
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.tdcolvin.gemmallmdemo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

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
                generateTerriblePoem = viewModel::generateTerriblePoem
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
    generateTerriblePoem: (String) -> Unit,
) {
    var poemSubject by remember { mutableStateOf(initialPoemSubject ?: "") }

    var showTakePhotoDialog by remember { mutableStateOf(false) }

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
            enabled = poemComplete
        ) {
            Text("Generate Terrible Poetry")
        }

        Poem(
            modifier = Modifier.padding(top = 20.dp),
            title = if (poemTitle.isNullOrBlank()) "" else """"$poemTitle"""",
            verses = poemVerse ?: "",
            complete = poemComplete
        )
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

data class TerriblePoemUiState(
    val loaded: Boolean = false,
    val poemTitle: String? = null,
    val poemVerse: String? = null,
    val poemComplete: Boolean = true
)

class TerriblePoemViewModel(application: Application): AndroidViewModel(application) {
    val uiState = MutableStateFlow(TerriblePoemUiState())

    private var llmInference: LlmInference? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Set the configuration options for the LLM Inference task
            val options = LlmInferenceOptions.builder()
                .setModelPath("/data/local/tmp/gemma2-2b-it-cpu-int8.task")
                .setMaxTokens(500)
                .setTemperature(1.0f)
                .setRandomSeed(Random.nextInt())
                .setResultListener { partialResult, done ->
                    uiState.update {
                        it.copy(
                            poemComplete = done,
                            poemVerse =  (it.poemVerse ?: "") + partialResult
                        )
                    }
                }
                .build()

            // Create an instance of the LLM Inference task
            llmInference = LlmInference.createFromOptions(application, options)
            uiState.update { it.copy(loaded = true) }
        }
    }

    fun generateTerriblePoem(poemSubject: String) {
        val prompt = "Write a single verse, terrible poem about the following subject: " +
                "${poemSubject}. It should be 4 lines long and almost, but not quite, rhyme. It " +
                "should be intentionally terrible, with bonus points for some factual inaccuracies. " +
                "Respond only with the 4 lines of the poem. Do not include any other text."

        uiState.update {
            it.copy(poemComplete = false, poemVerse = "", poemTitle = poemSubject)
        }
        viewModelScope.launch(Dispatchers.IO) {
            llmInference?.generateResponseAsync(prompt)
        }
    }
}