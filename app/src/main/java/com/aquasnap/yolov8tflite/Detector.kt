package com.aquasnap.yolov8tflite

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener
) {

    // TensorFlow Lite interpreter instance
    private var interpreter: Interpreter? = null

    // Labels and descriptions parsed from assets
    private var labels = mutableListOf<String>()
    private var descriptions = mutableMapOf<String, String>()

    // Model input/output shapes
    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    // Image processor to normalize and cast image input
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    // Load model and initialize interpreter
    fun setup() {
        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options().apply { numThreads = 4 }
        interpreter = Interpreter(model, options)

        // Read model input/output shapes
        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return
        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        numChannel = outputShape[1]
        numElements = outputShape[2]

        // Load labels and class descriptions
        loadLabelsFromAssets(labelPath)
        loadDescriptionsFromAssets("labelsinfo.txt")
    }

    // Load class labels from assets
    private fun loadLabelsFromAssets(fileName: String) {
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                labels.add(line)
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Load label descriptions from formatted text in assets (format: label:description)
    private fun loadDescriptionsFromAssets(fileName: String) {
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line?.split(":") ?: continue
                if (parts.size == 2) {
                    descriptions[parts[0].trim()] = parts[1].trim()
                }
            }
            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Free interpreter resources
    fun clear() {
        interpreter?.close()
        interpreter = null
    }

    // Perform detection on a bitmap frame
    fun detect(frame: Bitmap) {
        // Skip detection if interpreter is not ready
        if (interpreter == null || tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        // Resize image to model input dimensions
        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        // Preprocess image
        val tensorImage = TensorImage(DataType.FLOAT32).apply { load(resizedBitmap) }
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        // Allocate output buffer
        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)

        // Run model inference
        interpreter?.run(imageBuffer, output.buffer)

        // Postprocess results
        val bestBoxes = bestBox(output.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // Notify listener of results
        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()
        } else {
            detectorListener.onDetect(bestBoxes, inferenceTime)
        }
    }

    // Extract best bounding boxes from model output
    private fun bestBox(array: FloatArray): List<BoundingBox>? {
        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            val confidencePercentage = (maxConf * 100).toInt()

            // Determine class name with fallback for unknowns
            val clsName = if (confidencePercentage < 70) {
                val similarClass = labels.getOrNull(maxIdx) ?: "Unknown"
                "Unknown - $confidencePercentage% Similar to - $similarClass"
            } else {
                labels.getOrNull(maxIdx) ?: "Unknown"
            }

            // Add valid detections only
            if (maxConf > CONFIDENCE_THRESHOLD) {
                if (clsName.startsWith("Unknown -") && clsName.contains("falsepos")) continue

                // Extract bounding box coordinates
                val cx = array[c]
                val cy = array[c + numElements]
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - w / 2F
                val y1 = cy - h / 2F
                val x2 = cx + w / 2F
                val y2 = cy + h / 2F

                // Discard out-of-bound boxes
                if (x1 < 0F || x1 > 1F || y1 < 0F || y1 > 1F || x2 < 0F || x2 > 1F || y2 < 0F || y2 > 1F) continue

                // Attach description if available
                val description = descriptions[clsName] ?: "No description available"

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName,
                        description = description,
                        confidencePercentage = confidencePercentage
                    )
                )
            }
        }

        return if (boundingBoxes.isEmpty()) null else applyNMS(boundingBoxes)
    }

    // Apply Non-Maximum Suppression to reduce overlapping boxes
    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.removeFirst()
            selectedBoxes.add(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                if (calculateIoU(first, nextBox) >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    // Calculate Intersection over Union (IoU) between two boxes
    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    // Interface for detection callbacks
    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    // Constants
    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.7F
        private const val IOU_THRESHOLD = 0.5F
    }
}
