package com.aquasnap.yolov8tflite

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FishHistoryDescriptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fish_history_description)

        // Back button functionality
        val exitButton = findViewById<ImageButton>(R.id.exitButton)
        exitButton.setOnClickListener {
            finish() // Close the current activity and go back to the previous one
        }

        // Get the fish details passed from the previous activity
        val fishName = intent.getStringExtra("fishName") ?: "Unknown Fish"
        val fishImageResId = intent.getIntExtra("fishImageResId",  android.R.drawable.ic_menu_report_image )
        val timestamp = intent.getStringExtra("timestamp") ?: "No Timestamp"

        // Get fish characteristics, max size, weight, and description based on the fish name
        val (characteristics, maxSize, maxWeight, description) = getFishDetails(fishName)

        // Get references to UI elements
        val fishImageView = findViewById<ImageView>(R.id.fishImageView)
        val fishNameTextView = findViewById<TextView>(R.id.fishNameTextView)
        val fishLocalNameTextView = findViewById<TextView>(R.id.fishLocalNameTextView)
        val fishSizeTextView = findViewById<TextView>(R.id.fishSizeTextView)
        val fishWeightTextView = findViewById<TextView>(R.id.fishWeightTextView)
        val fishDescriptionTextView = findViewById<TextView>(R.id.fishDescriptionTextView)

        // Set values to the views
        fishImageView.setImageResource(fishImageResId)
        fishNameTextView.text = fishName
        fishLocalNameTextView.text = fishName  // Modify if you have local names
        fishSizeTextView.text = maxSize
        fishWeightTextView.text = maxWeight
        fishDescriptionTextView.text = description
    }

    // Function to get fish details based on name
    private fun getFishDetails(fishName: String): FishDetails {
        return when (fishName) {
            "Humphead Wrasse" -> FishDetails(
                "This gentle giant is one of the largest coral reef fish, recognized by its hump-shaped head and fleshy lips, and it plays a critical role in reef ecosystems by preying on destructive species like the crown-of-thorns starfish.",
                "2.1 m", "181 kg",
                "The humphead wrasse is a large reef fish with a prominent forehead hump and striking blue-green coloration.",
                R.drawable.wrasse
            )
            "Yellowtail Fusilier" -> FishDetails(
                "These fish are swift swimmers that feed on plankton, making them essential in the reef food chain.",
                "30 cm", "0.3 kg",
                "The yellowtail fusilier is a slender, torpedo-shaped fish with a distinctive yellow tail and a metallic blue body.",
                R.drawable.fusilier
            )
            "Trigger Fish" -> FishDetails(
                "Known for their unique dorsal fin 'trigger' mechanism, which locks them in place when threatened, these fish have powerful jaws for crushing shellfish.",
                "1 m", "6 kg",
                "Trigger fish are oval-shaped, compact fish with tough, leathery skin, often found near coral reefs.",
                R.drawable.pugot
            )
            "Goatfish" -> FishDetails(
                "Typically found in tropical and subtropical waters, goatfish have bright colors and an unusual ability to change hues depending on their environment.",
                "76 cm", "4.5 kg",
                "Goatfish are medium-sized, bottom-dwelling fish characterized by their long, whisker-like barbels that they use to probe the seafloor for food.",
                R.drawable.salmonito
            )
            "Yellowfin Tuna" -> FishDetails(
                "Known for their speed and endurance, yellowfin tuna are powerful migratory fish prized by sports fishers and are often seen leaping above the water's surface.",
                "280 cm", "400 kg",
                "Yellowfin Tuna is a species of tuna found in pelagic waters of tropical and subtropical oceans worldwide.",
                R.drawable.ahi
            )
            "Needle Fish" -> FishDetails(
                "These fish are fast swimmers and surface-dwellers, often seen leaping out of the water in bursts of speed, and are found in warm, shallow waters around coral reefs.",
                "1 m", "0.9 kg",
                "Needle fish are slender, elongated fish with a long beak filled with sharp teeth, giving them a distinctive, needle-like appearance.",
                R.drawable.tapog
            )
            "Rabbit Fish" -> FishDetails(
                "Rabbitfish, also known as 'spinefoots,' are small reef fish with elongated bodies and a characteristic pointed snout.",
                "30 cm", "1 kg",
                "These fish are known for their calm temperament and herbivorous diet, feeding primarily on algae and contributing to the balance of reef ecosystems.",
                R.drawable.kitong
            )
            "Sea Bass Fish" -> FishDetails(
                "These fish are versatile, inhabiting both coastal and deep-sea waters, and are known for their carnivorous diet, feeding on smaller fish, crustaceans, and cephalopods.",
                "6 m", "225 kg",
                "Sea bass is a general term for various species of marine fish, often with a robust body and muted coloration that allows them to blend into rocky or sandy habitats.",
                R.drawable.apahap
            )
            "Parrot Fish" -> FishDetails(
                "Known for their bright colors and unique feeding habits, parrot fish help maintain the health of coral reefs by removing algae, and they often sleep in a cocoon of mucus at night.",
                "1.2 m", "9 kg",
                "Parrot fish are vibrant, colorful fish with fused teeth that form a beak-like structure, used to scrape algae off coral reefs.",
                R.drawable.mulmul
            )
            "Mackerel Tuna" -> FishDetails(
                "Known for its strong swimming abilities and schooling behavior, tulingan is often found in tropical and subtropical waters.",
                "60 cm", "4 kg",
                "A streamlined, fast-swimming tuna species, the tulingan has a metallic blue back with dark stripes, fading to a silvery belly.",
                R.drawable.tulingan
            )
            else -> FishDetails(
                "No characteristics available.",
                "Unknown", "Unknown",
                "No description available.",
                android.R.drawable.ic_menu_report_image  // Provide a default image if no match is found
            )
        }
    }

    // Data class to hold fish details
    data class FishDetails(
        val characteristics: String,
        val maxSize: String,
        val maxWeight: String,
        val description: String,
        val imageResId: Int
    )
}

