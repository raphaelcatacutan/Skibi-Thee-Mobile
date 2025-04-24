package com.plm.skibithee

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plm.skibithee.ui.theme.SkibiTheeTheme
import java.io.File

// Jetpack Compose
// OkHttp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response

// File I/O
import java.io.IOException

// Logging
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkibiTheeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    val context = LocalContext.current
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val file = File(context.cacheDir, "upload.jpg")

                            inputStream?.use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            val requestBody = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("image", file.name,
                                    file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                .build()

                            val request = Request.Builder()
                                .url("http://192.168.1.13:3000/upload")
                                .post(requestBody)
                                .build()

                            val client = OkHttpClient()
                            client.newCall(request).enqueue(object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e("Upload", "Failed", e)
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    Log.i("Upload", "Success: ${response.body?.string()}")
                                }
                            })
                        }
                    }

                    // ✅ Add this to see your button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(onClick = {
                            launcher.launch("image/*")
                        }) {
                            Text("Pick and Upload Image")
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SkibiTheeTheme {
        Greeting("Android")
    }
}