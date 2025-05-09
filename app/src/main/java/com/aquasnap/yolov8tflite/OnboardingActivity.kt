package com.aquasnap.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Find the "Get Started" button by its ID
        val getStartedButton = findViewById<Button>(R.id.button)

        // Set a click listener on the button
        getStartedButton.setOnClickListener {
            startMainActivity() // Call the function to navigate to MainActivity
        }
    }

    // Function to navigate to MainActivity
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        // Optionally finish the onboarding activity so it can't be returned to
        finish()
    }
}
