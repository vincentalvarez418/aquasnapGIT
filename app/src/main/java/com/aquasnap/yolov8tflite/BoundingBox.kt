package com.aquasnap.yolov8tflite

/**
 * Represents a detected bounding box from an object detection model.
 *
 * @property x1 Top-left X coordinate
 * @property y1 Top-left Y coordinate
 * @property x2 Bottom-right X coordinate
 * @property y2 Bottom-right Y coordinate
 * @property cx Center X coordinate of the bounding box
 * @property cy Center Y coordinate of the bounding box
 * @property w Width of the bounding box
 * @property h Height of the bounding box
 * @property cnf Confidence score (0.0 to 1.0) of the detection
 * @property cls Integer class index of the detected object
 * @property clsName Name of the detected class (e.g., "person", "car")
 * @property description Additional human-readable description or label
 * @property confidencePercentage Confidence score represented as a percentage (0–100)
 */

data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val cls: Int,
    val clsName: String,
    val description: String,
    val confidencePercentage: Int
)
