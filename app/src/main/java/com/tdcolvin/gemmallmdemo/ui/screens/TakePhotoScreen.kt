package com.tdcolvin.gemmallmdemo.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.tdcolvin.gemmallmdemo.ui.components.CameraPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun TakePhotoScreen(
    modifier: Modifier = Modifier,
    setPhotoSubject: (String) -> Unit,
    viewModel: TakePhotoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val imageCaptureUseCase = remember { ImageCapture.Builder().build() }

    Column(modifier = modifier) {
        val image = uiState.image
        if (image != null) {
            Image(bitmap = image.asImageBitmap(), contentDescription = null)
            Text("Found: ${uiState.categoryFound ?: "<None>"}")

            Row {
                Button(onClick = { setPhotoSubject(uiState.categoryFound ?: "") }) {
                    Text("OK")
                }

                Button(onClick = { viewModel.setCameraImage(null) }) {
                    Text("Try again")
                }
            }
        }
        else {
            CameraPreview(
                modifier = Modifier.fillMaxWidth().weight(1f),
                imageCaptureUseCase = imageCaptureUseCase
            )
            Button(onClick = {
                imageCaptureUseCase.takePicture(
                    context.mainExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            super.onCaptureSuccess(image)
                            viewModel.setCameraImage(image)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("MainActivity", "Image capture failed", exception)
                        }
                    }
                )
            }) {
                Text("Capture")
            }
        }
    }
}

data class TakePhotoUiState(
    val categoryFound: String? = null,
    val image: Bitmap? = null,
)

class TakePhotoViewModel(application: Application): AndroidViewModel(application) {
    val uiState = MutableStateFlow(TakePhotoUiState())

    fun setCameraImage(image: ImageProxy?) {
        if (image == null) {
            uiState.update { it.copy(image = null, categoryFound = null) }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val inputWidth = 260
            val inputHeight = 260

            // Create a bitmap that's scaled as needed for the model, and rotated as needed to match display orientation
            val scaleAndRotate = Matrix().apply {
                postScale(inputWidth.toFloat() / image.width, inputHeight.toFloat() / image.height)
                postRotate(image.imageInfo.rotationDegrees.toFloat())
            }
            val bmp = Bitmap.createBitmap(image.toBitmap(), 0, 0, image.width, image.height, scaleAndRotate, true)

            image.close()

            uiState.update { it.copy(image = bmp) }

            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder().setModelAssetPath("efficientnet_lite2.tflite").build()
                )
                .setRunningMode(RunningMode.IMAGE)
                .setScoreThreshold(0.1f)
                .setMaxResults(10)
                .build()

            val imageClassifier = ImageClassifier.createFromOptions(getApplication(), options)
            val result = imageClassifier.classify(BitmapImageBuilder(bmp).build()).classificationResult()
            Log.v("classifications", "num classifications = ${result.classifications().size}")

            val classification = result.classifications().firstOrNull()
            val bestCategory = classification?.categories()?.maxByOrNull { it.score() }
            uiState.value = uiState.value.copy(categoryFound = bestCategory?.categoryName())
        }
    }
}