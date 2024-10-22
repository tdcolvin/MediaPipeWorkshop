package com.tdcolvin.gemmallmdemo.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.tdcolvin.gemmallmdemo.ui.components.CameraPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

@Composable
fun ReactionGestureScreen(
    modifier: Modifier = Modifier,
    setGesture: (String?) -> Unit,
    viewModel: ReactionGestureViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val imageAnalysisUseCase = remember {
        ImageAnalysis.Builder().build().apply {
            setAnalyzer(context.mainExecutor, viewModel.imageAnalyzer)
        }
    }

    Box(modifier = modifier) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            imageAnalysisUseCase = imageAnalysisUseCase
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            uiState.mostRecentGesture?.let {
                Text(
                    text = it,
                    fontSize = 100.sp
                )
            }

            Button(onClick = { setGesture(uiState.mostRecentGesture) }) {
                Text("Save")
            }
        }

    }
}

data class ReactionGestureUiState(
    val mostRecentGesture: String? = null,
)

class ReactionGestureViewModel(application: Application): AndroidViewModel(application) {
    val uiState = MutableStateFlow(ReactionGestureUiState())

    private val gestureRecognizer by lazy {
        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("gesture_recognizer.task")
        val baseOptions = baseOptionsBuilder.build()

        val optionsBuilder =
            GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setResultListener { result, _ ->
                    val bestCategory = result.gestures().firstOrNull()?.maxByOrNull { it.score() }
                    val gesture = when(bestCategory?.categoryName()) {
                        "Thumb_Up" -> "\uD83D\uDC4D"
                        "Thumb_Down" -> "\uD83D\uDC4E"
                        "Pointing_Up" -> "☝\uFE0F"
                        "Open_Palm" -> "✋"
                        "Closed_Fist" -> "✊"
                        "Victory" -> "✌\uFE0F"
                        "ILoveYou" -> "\uD83E\uDD1F"
                        else -> null
                    }

                    if (gesture != null) {
                        uiState.update { it.copy(mostRecentGesture = gesture) }
                    }
                }
                .setRunningMode(RunningMode.LIVE_STREAM)

        val options = optionsBuilder.build()
        GestureRecognizer.createFromOptions(getApplication(), options)
    }

    val imageAnalyzer by lazy {
        ImageAnalysis.Analyzer { image ->
                val scale = 500f / max(image.width, image.height)
                // Create a bitmap that's scaled as needed for the model, and rotated as needed to match display orientation
                val scaleAndRotate = Matrix().apply {
                    postScale(scale, scale)
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val bmp = Bitmap.createBitmap(image.toBitmap(), 0, 0, image.width, image.height, scaleAndRotate, true)

                image.close()

                gestureRecognizer.recognizeAsync(BitmapImageBuilder(bmp).build(), System.currentTimeMillis())
        }
    }
}