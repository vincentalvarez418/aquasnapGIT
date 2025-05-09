package com.aquasnap.yolov8tflite

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyList: MutableList<FishHistoryItem>  // Make the list mutable
    private lateinit var adapter: HistoryAdapter  // Declare adapter as a class-level variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val historyListView: ListView = findViewById(R.id.historyListView)
        val sortSpinner: Spinner = findViewById(R.id.sortSpinner)  // Get the Spinner for sorting

        // Get the detection history passed from MainActivity
        historyList = intent.getParcelableArrayListExtra<FishHistoryItem>("historyList")?.toMutableList() ?: mutableListOf()

        // Initialize the adapter
        adapter = HistoryAdapter(this, historyList) { position ->
            // Delete the item from the list
            historyList.removeAt(position)

            // Notify the adapter about the change
            adapter.notifyDataSetChanged()

            // Optionally, save the updated list to SharedPreferences
            saveHistoryToSharedPreferences(historyList)
        }

        // Set the adapter to the ListView
        historyListView.adapter = adapter

        // Set up Spinner for sorting options
        val sortOptions = arrayOf("Sort by New", "Sort by Old")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = spinnerAdapter

        // Listen for changes in sorting option
        sortSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> sortHistoryAscending()  // Sort in ascending order
                    1 -> sortHistoryDescending()  // Sort in descending order
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case where nothing is selected (optional)
            }
        })
    }

    // Sort the history list in ascending order (based on timestamp)
    private fun sortHistoryAscending() {
        historyList.sortBy { it.timestamp }  // Sort by timestamp in ascending order
        adapter.notifyDataSetChanged()  // Notify the adapter that the data has changed
    }

    // Sort the history list in descending order (based on timestamp)
    private fun sortHistoryDescending() {
        historyList.sortByDescending { it.timestamp }  // Sort by timestamp in descending order
        adapter.notifyDataSetChanged()  // Notify the adapter that the data has changed
    }

    // Function to save updated history to SharedPreferences
    private fun saveHistoryToSharedPreferences(historyList: List<FishHistoryItem>) {
        val sharedPreferences = getSharedPreferences("DetectionHistory", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()  // Create a Gson object

        // Convert the list to JSON
        val historyJson = gson.toJson(historyList)

        // Save the updated history to SharedPreferences
        editor.putString("historyList", historyJson)
        editor.apply()

        Toast.makeText(this, "History Updated", Toast.LENGTH_SHORT).show()
    }
}
