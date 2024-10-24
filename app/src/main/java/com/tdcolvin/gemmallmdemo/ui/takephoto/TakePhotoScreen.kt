package com.tdcolvin.gemmallmdemo.ui.takephoto

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcolvin.gemmallmdemo.ui.components.CameraPreview

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