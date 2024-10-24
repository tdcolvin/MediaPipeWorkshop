package com.tdcolvin.gemmallmdemo.ui.takephoto

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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