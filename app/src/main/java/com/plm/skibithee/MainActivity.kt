package com.plm.skibithee

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.plm.skibithee.ui.theme.SkibiTheeTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SkibiTheeTheme {
                var appMessage by remember { mutableStateOf("") }
                var username by remember { mutableStateOf("") }
                var photoUri by remember { mutableStateOf<Uri?>(null) }
                var photoFile by remember { mutableStateOf<File?>(null) }

                val context = LocalContext.current

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success) {
                        // Only update photoUri after successful capture
                        Log.d("Camera", "Captured Image URI: $photoUri")
                    } else {
                        // Reset photoUri if the camera capture was unsuccessful
                        photoUri = null
                    }
                }

                fun createImageFile(): File {
                    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.cacheDir)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // TextField to enter username
                            TextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Enter your name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Capture Image button
                            Button(onClick = {
                                // Reset photoUri before launching the camera intent to avoid showing a blank image
                                photoUri = null

                                val file = createImageFile()
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                photoFile = file
                                photoUri = uri // Set URI before launching the camera

                                cameraLauncher.launch(uri)
                            }) {
                                Text(if (photoUri == null) "Capture Image" else "Retake Image")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Display captured image only if photoUri is not null
                            photoUri?.let { uri ->
                                val painter = rememberAsyncImagePainter(model = uri)
                                Image(
                                    painter = painter,
                                    contentDescription = "Captured image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Send button to upload image
                            Button(onClick = {
                                val file = photoFile
                                if (file != null && username.isNotBlank()) {
                                    val requestBody = MultipartBody.Builder()
                                        .setType(MultipartBody.FORM)
                                        .addFormDataPart("username", username)
                                        .addFormDataPart(
                                            "image", file.name,
                                            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                        )
                                        .build()

                                    val request = Request.Builder()
                                        .url("http://192.168.1.13:3000/upload")
                                        .post(requestBody)
                                        .build()

                                    OkHttpClient().newCall(request).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            Log.e("Upload", "Failed", e)
                                            appMessage = "Upload Failed"
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            Log.i("Upload", "Success: ${response.body?.string()}")
                                            appMessage = "Upload Success"
                                        }
                                    })
                                } else {
                                    Log.w("Validation", "Name or image is empty")
                                    appMessage = "Name or image is empty"
                                }
                            }) {
                                Text("Send")
                            }

                            // Display app message after action
                            Text(appMessage)
                        }
                    }
                }
            }
        }
    }
}
