package com.example.plantapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
data class PredictionResponse(
    val img_path: String,
    val prediction: String
)


class MainActivity : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 100
    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private var imageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        imageView = findViewById(R.id.imageView)
        textViewResult = findViewById(R.id.textViewResult)
        val buttonCapture = findViewById<Button>(R.id.buttonCapture)
        val buttonUpload = findViewById<Button>(R.id.buttonUpload)

        // Handle the capture button click
        buttonCapture.setOnClickListener {
            if (isCameraPermissionGranted()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        // Handle the upload button click
        buttonUpload.setOnClickListener {
            imageFile?.let { uploadImage(it) }
                ?: Toast.makeText(this, "Capture an image first!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    private fun openCamera() {
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(bitmap)

            // Save bitmap to file
            val file = File(cacheDir, "image.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
            imageFile = file
        }
    }

    private fun uploadImage(file: File) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://8f3b-2405-201-4024-c009-7831-51fe-3ff4-3349.ngrok-free.app/") // Replace with your ngrok URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        api.uploadImage(body).enqueue(object : retrofit2.Callback<PredictionResponse> {
            override fun onResponse(call: retrofit2.Call<PredictionResponse>, response: retrofit2.Response<PredictionResponse>) {
                if (response.isSuccessful) {
                    val prediction = response.body()?.prediction
                    textViewResult.text = "Prediction: $prediction"
                } else {
                    textViewResult.text = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: retrofit2.Call<PredictionResponse>, t: Throwable) {
                textViewResult.text = "Failed to connect: ${t.message}"
            }
        })
    }

}
