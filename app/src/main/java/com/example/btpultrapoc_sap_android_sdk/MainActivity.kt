package com.example.btpultrapoc_sap_android_sdk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val REQUEST_IMAGE_CAPTURE = 1
    private val SELECT_PICTURE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonCamera: Button = findViewById(R.id.button_camera)
        val buttonGallery: Button = findViewById(R.id.button_gallery)


        buttonCamera.setOnClickListener{
            takePicture()
        }

        buttonGallery.setOnClickListener {
            choosePicture()
        }
    }

    private fun takePicture() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_IMAGE_CAPTURE
            )
        } else {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun choosePicture() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                val photo = data?.extras?.get("data") as Bitmap
                uploadImage(photo)
            } else if (requestCode == SELECT_PICTURE) {
                val imageUri = data?.data
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                uploadImage(bitmap)
            }
        }
    }

    private fun uploadImage(image: Bitmap) {
        val imageName = "testImageName.jpg";
        val byteArrayOutputStream = ByteArrayOutputStream()
        image.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            byteArrayOutputStream
        )

        val requestBody = RequestBody.create(
            "image/jpeg".toMediaTypeOrNull(),
            byteArrayOutputStream.toByteArray()
        )

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", imageName, requestBody)
            .build()

        val initRequest = okhttp3.Request.Builder()
            .url("https://btpultrapoc.requestcatcher.com/test")
            .post(multipartBody)
            .build()

        client.newCall(initRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val id = response.body?.string() ?: throw IOException("No ID returned")

                // Step 2: Use the ID to update the entity with a PUT request that includes the image file
                val uploadRequest = okhttp3.Request.Builder()
                    .url("https://your-cap-service-endpoint/update/$id")
                    .put(multipartBody)
                    .build()

                client.newCall(uploadRequest).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use {
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            println(response.body?.string())
                        }
                    }
                })
            }
        })
    }
}
