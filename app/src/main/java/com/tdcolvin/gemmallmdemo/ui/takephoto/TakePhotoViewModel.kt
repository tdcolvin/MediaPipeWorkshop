package com.tdcolvin.gemmallmdemo.ui.takephoto

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.ClassificationResult
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

    // This function is called when the user hits the Capture button
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

            val result = classifyImage(bmp)
            Log.v("classifications", "num classifications = ${result?.classifications()?.size}")

            // Pick the first thing the model has seen in the image
            val classification = result?.classifications()?.firstOrNull()

            // Pick the most likely category (i.e. the one with the highest probabilistic score)
            val bestCategory = classification?.categories()?.maxByOrNull { it.score() }

            // Display it on the UI
            uiState.value = uiState.value.copy(categoryFound = bestCategory?.categoryName())
        }
    }

    private fun classifyImage(bitmap: Bitmap): ClassificationResult? {
        /*
        TODO: Write the code to find what's in the bitmap image.

        Hints:
           * You need an instance of ImageClassifier which you can get using
             ImageClassifier.createFromOptions(...).

           * There's an example of how to do that here:
             https://ai.google.dev/edge/mediapipe/solutions/vision/image_classifier/android#create_the_task

           * The model asset path you need is "efficientnet_lite2.tflite". I also recommend a low
             score threshold (say 0.1) - in our case, any result is better than nothing.

           * You will need to convert the bitmap to a format that the model uses. To do that, use
             BitmapImageBuilder(bitmap).build()

           * Once you've created the image classifier instance, you use .classify() on it.

           * To see a full example, check out the "answers" branch
        */

        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(
                BaseOptions.builder().setModelAssetPath("efficientnet_lite2.tflite").build()
            )
            .setRunningMode(RunningMode.IMAGE)
            .setScoreThreshold(0.1f)
            .setMaxResults(10)
            .build()

        val imageClassifier = ImageClassifier.createFromOptions(getApplication(), options)
        return imageClassifier.classify(BitmapImageBuilder(bitmap).build()).classificationResult()
    }
}