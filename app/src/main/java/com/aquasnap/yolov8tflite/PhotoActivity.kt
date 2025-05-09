package com.aquasnap.yolov8tflite

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment // <-- Add this import
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aquasnap.yolov8tflite.databinding.ActivityPhotoBinding

class PhotoActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityPhotoBinding
    private lateinit var detector: Detector
    private lateinit var overlayView: OverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize OverlayView
        overlayView = OverlayView(this, null)
        binding.root.addView(overlayView)

        // Retrieve the photo URI from the intent
        val photoUri = intent.getStringExtra("photoUri")

        // Load the image from the URI into an ImageView
        val imageView: ImageView = binding.imageView
        imageView.setImageURI(Uri.parse(photoUri))

        // Initialize the detector with constants for model and labels
        detector = Detector(this, Constants.MODEL_PATH, Constants.LABELS_PATH, this)
        detector.setup()

        // Load the bitmap from the ImageView to detect objects
        imageView.post {
            val bitmap = (imageView.drawable as BitmapDrawable).bitmap
            detectObjects(bitmap)
        }

        // Save button listener
        binding.saveButton.setOnClickListener {
            saveImageToGallery()
        }
    }

    private fun detectObjects(bitmap: Bitmap) {
        detector.detect(bitmap)
    }

    private fun saveImageToGallery() {
        val imageView: ImageView = binding.imageView
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap

        // Create a content resolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Image_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // Save in Pictures directory
        }

        // Insert the image into the MediaStore
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    showToast("Image saved to gallery!")
                } else {
                    showToast("Error saving image.")
                }
            }
        } ?: showToast("Error creating content values.")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        overlayView.setResults(boundingBoxes)
    }

    override fun onEmptyDetect() {
        overlayView.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
    }
}
