package com.aquasnap.yolov8tflite

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FishDescriptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fish_description)

        // Handle the back button press
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Return to previous activity
        }

        // Retrieve fish data from Intent extras
        val fishName = intent.getStringExtra("fishName") ?: "Unknown Fish"
        val fishLocalName = intent.getStringExtra("fishLocalName") ?: "Unknown"
        val fishImageResId = intent.getIntExtra("fishImageResId", R.drawable.default_fish)

        // Variables for fish details
        val characteristics: String
        val maxSize: String
        val maxWeight: String
        val description: String

        // Assign values based on fish name
        when (fishName) {
            "Humphead Wrasse" -> {
                characteristics = "This gentle giant is one of the largest coral reef fish, recognized by its hump-shaped head and fleshy lips, and it plays a critical role in reef ecosystems by preying on destructive species like the crown-of-thorns starfish."
                maxSize = "2.1 m"
                maxWeight = "181 kg"
                description = "The humphead wrasse is a large reef fish with a prominent forehead hump and striking blue-green coloration."
            }
            "Yellowtail Fusilier" -> {
                characteristics = "These fish are swift swimmers that feed on plankton, making them essential in the reef food chain."
                maxSize = "30 cm"
                maxWeight = "0.3 kg"
                description = "The yellowtail fusilier is a slender, torpedo-shaped fish with a distinctive yellow tail and a metallic blue body."
            }
            "Trigger Fish" -> {
                characteristics = "Known for their unique dorsal fin \"trigger\" mechanism, which locks them in place when threatened, these fish have powerful jaws for crushing shellfish."
                maxSize = "1 m"
                maxWeight = "6 kg"
                description = "Trigger fish are oval-shaped, compact fish with tough, leathery skin, often found near coral reefs."
            }
            "Goatfish" -> {
                characteristics = "Typically found in tropical and subtropical waters, goatfish have bright colors and an unusual ability to change hues depending on their environment"
                maxSize = "76 cm"
                maxWeight = "4.5 kg"
                description = "Goatfish are medium-sized, bottom-dwelling fish characterized by their long, whisker-like barbels that they use to probe the seafloor for food."
            }
            "Yellowfin Tuna" -> {
                characteristics = "Known for their speed and endurance, yellowfin tuna are powerful migratory fish prized by sports fishers and are often seen leaping above the water's surface."
                maxSize = "280 cm"
                maxWeight = "400 kg"
                description = "Yellowfin Tuna is a species of tuna found in pelagic waters of tropical and subtropical oceans worldwide."
            }
            "Needle Fish" -> {
                characteristics = "These fish are fast swimmers and surface-dwellers, often seen leaping out of the water in bursts of speed, and are found in warm, shallow waters around coral reefs."
                maxSize = "1 m"
                maxWeight = "0.9 kg"
                description = "Needle fish are slender, elongated fish with a long beak filled with sharp teeth, giving them a distinctive, needle-like appearance."
            }
            "Rabbit Fish" -> {
                characteristics = "Rabbitfish, also known as \"spinefoots,\" are small reef fish with elongated bodies and a characteristic pointed snout."
                maxSize = "30 cm"
                maxWeight = "1 kg"
                description = "These fish are known for their calm temperament and herbivorous diet, feeding primarily on algae and contributing to the balance of reef ecosystems."
            }
            "Sea Bass Fish" -> {
                characteristics = "These fish are versatile, inhabiting both coastal and deep-sea waters, and are known for their carnivorous diet, feeding on smaller fish, crustaceans, and cephalopods."
                maxSize = "6 m"
                maxWeight = "225 kg"
                description = "Sea bass is a general term for various species of marine fish, often with a robust body and muted coloration that allows them to blend into rocky or sandy habitats."
            }
            "Parrot Fish" -> {
                characteristics = "Known for their bright colors and unique feeding habits, parrot fish help maintain the health of coral reefs by removing algae, and they often sleep in a cocoon of mucus at night."
                maxSize = "1.2 m"
                maxWeight = "9 kg"
                description = "Parrot fish are vibrant, colorful fish with fused teeth that form a beak-like structure, used to scrape algae off coral reefs."
            }
            "Mackerel Tuna" -> {
                characteristics = "Known for its strong swimming abilities and schooling behavior, tulingan is often found in tropical and subtropical waters."
                maxSize = "60 cm"
                maxWeight = "4 kg"
                description = "A streamlined, fast-swimming tuna species, the tulingan has a metallic blue back with dark stripes, fading to a silvery belly."
            }
            else -> {
                characteristics = "Unknown characteristics"
                maxSize = "Unknown size"
                maxWeight = "Unknown weight"
                description = "No description available."
            }
        }

        // Populate UI elements with fish details
        val fishImageView = findViewById<ImageView>(R.id.fishImageView)
        val fishNameTextView = findViewById<TextView>(R.id.fishNameTextView)
        val fishLocalNameTextView = findViewById<TextView>(R.id.fishLocalNameTextView)
        val fishSizeTextView = findViewById<TextView>(R.id.fishSizeTextView)
        val fishWeightTextView = findViewById<TextView>(R.id.fishWeightTextView)
        val fishDescriptionTextView = findViewById<TextView>(R.id.fishDescriptionTextView)
        val fishCharacteristicsTextView = findViewById<TextView>(R.id.fishCharacteristicsTextView)

        fishImageView.setImageResource(fishImageResId)
        fishNameTextView.text = fishName
        fishLocalNameTextView.text = fishLocalName
        fishSizeTextView.text = maxSize
        fishWeightTextView.text = maxWeight
        fishDescriptionTextView.text = description
        fishCharacteristicsTextView.text = characteristics
    }
}
