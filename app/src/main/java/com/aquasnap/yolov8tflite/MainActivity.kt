package com.aquasnap.yolov8tflite

import android.Manifest
import com.google.gson.reflect.TypeToken
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.graphics.Color

import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aquasnap.yolov8tflite.Constants.LABELS_PATH
import com.aquasnap.yolov8tflite.Constants.MODEL_PATH
import com.aquasnap.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.gson.Gson
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import android.view.Gravity


class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var detectedCountTextView: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var detectionHistory = mutableListOf<FishData>() // Store FishData objects
    private lateinit var detector: Detector
    private var detectionEnabled = false // Initialize detection state as OFF
    private val isFrontCamera = false
    private var isPreviewPaused = false  // To track whether the preview is paused or not
    private var lastSaveTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components
        detectedCountTextView = binding.detectedCount

        detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set the bounding box click listener in the OverlayView
        binding.overlay.setOnBoundingBoxClickListener { boundingBox: BoundingBox ->
            showBoundingBoxInfoPopup(boundingBox)
        }

        // **Detection Toggle Button**
        val toggleDetectionButton = findViewById<Button>(R.id.toggleDetectionButton)
        toggleDetectionButton.setOnClickListener {
            detectionEnabled = !detectionEnabled
            toggleDetectionButton.text = if (detectionEnabled) "Hide Detections" else "Show Detections"

            // Clear overlay and UI when detection is disabled
            if (!detectionEnabled) {
                // Stop camera feed by unbinding all use cases
                val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()  // Unbind all use cases (preview, analyzer, etc.)

                    // Optionally, reset the overlay and detected count
                    binding.overlay.clear()
                    binding.overlay.invalidate()
                    detectedCountTextView.text = "Detected Items: 0"

                    // Restart camera feed
                    startCamera()  // This will rebind the use cases and restart the camera
                }, ContextCompat.getMainExecutor(this))
            }
        }

        val pauseResumeButton = findViewById<ImageView>(R.id.pauseResumeButton)
        pauseResumeButton.setOnClickListener {
            if (isPreviewPaused) {
                // Resume Preview
                startCamera()
                pauseResumeButton.setImageResource(R.drawable.snap) // Replace with your "resume" icon
            } else {
                // Pause Preview
                val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll() // Unbind the camera use cases to pause the preview
                }, ContextCompat.getMainExecutor(this))

                pauseResumeButton.setImageResource(R.drawable.snap) // Replace with your "pause" icon
            }
            isPreviewPaused = !isPreviewPaused // Toggle the paused state
        }


        val historyStatusButton = findViewById<ImageView>(R.id.historyStatusButton)
        historyStatusButton.setOnClickListener {
            // Display the history of detections
            showDetectionHistory()
        }


        // Set OnClickListener for the bar icon to navigate to LibraryActivity
        val barIcon = findViewById<ImageView>(R.id.imageView3)
        barIcon.setOnClickListener {
            val intent = Intent(this, LibraryActivity::class.java)
            startActivity(intent)
        }


    }

    private fun showDetectionHistory() {
        loadHistoryFromSharedPreferences()  // Load the history from SharedPreferences

        if (detectionHistory.isEmpty()) {
            Toast.makeText(this, "No detections yet!", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, HistoryActivity::class.java)

            // Convert FishData objects to FishHistoryItem objects
            val historyList = detectionHistory.map { fish ->
                val timestamp = fish.fishDescription.substringAfterLast("Timestamp: ", "Unknown Timestamp")
                FishHistoryItem(fish.fishName, fish.fishImageResId, timestamp)
            }

            // Pass the history list (FishHistoryItem) to the HistoryActivity
            intent.putParcelableArrayListExtra("historyList", ArrayList(historyList))
            startActivity(intent)
        }
    }




    private fun showBoundingBoxInfoPopup(boundingBox: BoundingBox) {
        val localName = boundingBox.clsName

        // Check if the classification indicates "Unknown"
        if (localName.startsWith("Unknown", ignoreCase = true)) {
            // Show Toast first, then show the dialog with similarity info
            Toast.makeText(this, "Unknown fish detected. Details not available.", Toast.LENGTH_SHORT).show()

            // Extract similar fish and percentage
            val confidencePercentage = boundingBox.confidencePercentage
            val similarFish = extractSimilarFish(localName)

            // Get the fish data (including image) for the similar fish
            val (similarFishName, _, _, similarFishImageResId) = mapFishData(similarFish)

            // Show the mini modal (dialog) with similar fish info and image
            showSimilarityDialog(similarFish, confidencePercentage, similarFishImageResId)

            return
        }

        // Proceed with normal behavior for non-Unknown fish
        val (fishName, fishLocalName, fishDescription, fishImageResId) = mapFishData(localName)

        // Create an Intent to open FishDetailActivity and pass the data
        val intent = Intent(this, FishDetailActivity::class.java).apply {
            putExtra("fishName", fishName)
            putExtra("fishLocalName", fishLocalName)
            putExtra("fishDescription", fishDescription)
            putExtra("fishImageResId", fishImageResId)
        }

        // Start FishDetailActivity
        startActivity(intent)

        // Add fish data to history
        val currentTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        detectionHistory.add(FishData(fishName, fishLocalName, "$fishDescription\nTimestamp: $currentTimestamp", fishImageResId))

        // Save the updated history to SharedPreferences
        saveHistoryToSharedPreferences()
    }


    private fun showSimilarityDialog(similarFish: String, confidencePercentage: Int, fishImageResId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Similar Fish Detection Result:")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }

        // Create a layout for the dialog content
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30) // Add padding inside the modal
            gravity = Gravity.CENTER
        }

        // Add the fish image to the layout
        val imageView = ImageView(this).apply {
            setImageResource(fishImageResId)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply {
                setMargins(0, 20, 0, 20) // Add margins around the image
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        layout.addView(imageView)

        // Add fish name and confidence text
        val textView = TextView(this).apply {
            text = "This fish is similar to:\n$similarFish\nConfidence: $confidencePercentage%"
            textSize = 18f
            setTextColor(Color.BLACK)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        layout.addView(textView)

        // Set the layout to the dialog
        builder.setView(layout)

        // Show the dialog
        builder.create().show()
    }

    private fun extractConfidencePercentage(clsName: String): Int {
        // Extract confidence percentage from the class name (format: Similar to - FishName 85%)
        return clsName.substringAfter("Similar to - ")
            .substringAfterLast(" ")
            .substringBefore("%")
            .toIntOrNull() ?: 0
    }

    private fun extractSimilarFish(clsName: String): String {
        // Extract similar fish name from the class name (format: Similar to - FishName 85%)
        return clsName.substringAfter("Similar to - ")
            .substringBeforeLast(" ")
    }








    // Mapping function to get the fish data based on the local name
    private fun mapFishData(localName: String): FishData {
        return when (localName) {
            "Mameng" -> FishData(
                "Humphead Wrasse", "Mameng", "The humphead wrasse is a large reef fish with a prominent forehead hump and striking blue-green coloration.",
                R.drawable.wrasse
            )
            "Fusilier" -> FishData(
                "Yellowtail Fusilier", "Fusilier", "The yellowtail fusilier is a slender, torpedo-shaped fish with a distinctive yellow tail and a metallic blue body.",
                R.drawable.fusilier
            )
            "Pugot" -> FishData(
                "Trigger Fish", "Pugot", "Trigger fish are oval-shaped, compact fish with tough, leathery skin, often found near coral reefs.",
                R.drawable.pugot
            )
            "Salmonito" -> FishData(
                "Goatfish", "Salmonito", "Goatfish are medium-sized, bottom-dwelling fish characterized by their long, whisker-like barbels that they use to probe the seafloor for food.",
                R.drawable.salmonito
            )
            "Ahi" -> FishData(
                "Yellowfin Tuna", "Ahi", "Yellowfin Tuna is a species of tuna found in pelagic waters of tropical and subtropical oceans worldwide.",
                R.drawable.ahi
            )
            "Tapog" -> FishData(
                "Needle Fish", "Tapog", "Needle fish are slender, elongated fish with a long beak filled with sharp teeth, giving them a distinctive, needle-like appearance.",
                R.drawable.tapog
            )
            "Kitong" -> FishData(
                "Rabbit Fish", "Kitong", "Rabbitfish, also known as 'spinefoots,' are small reef fish with elongated bodies and a characteristic pointed snout.",
                R.drawable.kitong
            )
            "Apahap" -> FishData(
                "Sea Bass Fish", "Apahap", "Sea bass is a general term for various species of marine fish, often with a robust body and muted coloration that allows them to blend into rocky or sandy habitats.",
                R.drawable.apahap
            )
            "Mulmul" -> FishData(
                "Parrot Fish", "Mulmul", "Parrot fish are vibrant, colorful fish with fused teeth that form a beak-like structure, used to scrape algae off coral reefs.",
                R.drawable.mulmul
            )
            "Tulingan" -> FishData(
                "Mackerel Tuna", "Tulingan", "A streamlined, fast-swimming tuna species, the tulingan has a metallic blue back with dark stripes, fading to a silvery belly.",
                R.drawable.tulingan
            )
            else -> FishData(
                "Unknown Fish", "Unknown", "No description available.", android.R.drawable.ic_menu_report_image
            )
        }
    }

    // A simple data class to hold fish data
    data class FishData(
        val fishName: String,
        val fishLocalName: String,
        val fishDescription: String,
        val fishImageResId: Int
    )

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    fun saveHistoryToSharedPreferences() {
        val currentTime = System.currentTimeMillis()
        val cooldownPeriod = 5000L // 5 seconds cooldown period

        // Check if the cooldown period has passed
        if (currentTime - lastSaveTimestamp < cooldownPeriod) {
            Toast.makeText(this, "Please wait before saving again.", Toast.LENGTH_SHORT).show()
            return
        }

        lastSaveTimestamp = currentTime // Update the last save timestamp

        // Filter out any "Unknown" fish from the detection history
        val validHistory = detectionHistory.filter {
            it.fishName != "Unknown Fish" && it.fishLocalName != "Unknown"
        }

        val sharedPreferences = getSharedPreferences("DetectionHistory", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        // Convert the filtered detectionHistory to JSON
        val historyJson = gson.toJson(validHistory)

        // Save JSON string to SharedPreferences
        editor.putString("historyList", historyJson)
        editor.apply()  // Save changes

        Toast.makeText(this, "History saved successfully.", Toast.LENGTH_SHORT).show()
    }




    fun loadHistoryFromSharedPreferences() {
        val sharedPreferences = getSharedPreferences("DetectionHistory", MODE_PRIVATE)
        val gson = Gson()
        val historyJson = sharedPreferences.getString("historyList", null)

        if (historyJson != null) {
            // Convert the JSON string back to a list of FishData objects
            val fishHistoryType = object : TypeToken<List<FishData>>() {}.type
            detectionHistory = gson.fromJson(historyJson, fishHistoryType)
        }
    }



    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val rotation = binding.viewFinder.display.rotation

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
            if (!detectionEnabled) {
                imageProxy.close()  // Skip processing if detection is disabled
                return@setAnalyzer
            }

            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector.detect(rotatedBitmap)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer, imageCapture
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }



    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEmptyDetect() {
        binding.overlay.clear()
        binding.overlay.invalidate()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        if (!detectionEnabled) return // Skip drawing bounding boxes if detection is disabled

        runOnUiThread {
            detectedCountTextView.text = "Detected Items: ${boundingBoxes.size}"
            binding.overlay.setResults(boundingBoxes)
            binding.overlay.invalidate()
        }
    }
}
