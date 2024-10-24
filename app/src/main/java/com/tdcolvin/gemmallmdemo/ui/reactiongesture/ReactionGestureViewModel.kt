package com.tdcolvin.gemmallmdemo.ui.reactiongesture

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

data class ReactionGestureUiState(
    val mostRecentGesture: String? = null,
)

class ReactionGestureViewModel(application: Application): AndroidViewModel(application) {
    val uiState = MutableStateFlow(ReactionGestureUiState())

    private val gestureRecognizer by lazy {
        /*
          TODO: Create a gesture recognizer here and use it to call handleGestureRecognizerResult
                below, whenever it has a result.

          Hints:
             * Use the same builder pattern as the last task, but this time using
               GestureRecognizer.createFromOptions(...)

             * There's an example here:
               https://ai.google.dev/edge/mediapipe/solutions/vision/gesture_recognizer/android#create_the_task
               Make sure you click "Live stream" on the code examples!

             * The model asset path is "gesture_recognizer.task"

             * Set the running mode to RunningMode.LIVE_STREAM

             * Because we're in live stream mode, you'll need to set a result listener

             * You will need to run this gesture recogniser - see extra to-do near the end of this
               file

             * Again, there's a full example on the "answers" branch
        */
    }

    private fun handleGestureRecognizerResult(result: GestureRecognizerResult) {
        // Figure out the most likely gesture
        val bestCategory = result.gestures().firstOrNull()?.maxByOrNull { it.score() }

        // Convert it to an emoji
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

        // Display it in the UI
        if (gesture != null) {
            uiState.update { it.copy(mostRecentGesture = gesture) }
        }
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

            /* TODO: call the recognizeAsync function of your gesture recognizer here */

        }
    }
}