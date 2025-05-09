package com.aquasnap.yolov8tflite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent

// Custom adapter to display a list of FishHistoryItem objects in a ListView
class HistoryAdapter(
    private val context: Context,                        // Context from the calling activity
    private val historyList: MutableList<FishHistoryItem>, // List of fish history records
    private val deleteItem: (Int) -> Unit                // Lambda function to delete an item by position
) : BaseAdapter() {

    // Returns the number of items in the history list
    override fun getCount(): Int = historyList.size

    // Returns the item at the specified position
    override fun getItem(position: Int): Any = historyList[position]

    // Returns the item's ID (here, just the position as a Long)
    override fun getItemId(position: Int): Long = position.toLong()

    // Creates and returns a view for each list item
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Reuse the existing view if available, else inflate a new one
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.history_item, parent, false)

        // Get references to views in the list item layout
        val fishNameTextView: TextView = view.findViewById(R.id.fishNameTextView)
        val fishImageView: ImageView = view.findViewById(R.id.fishImageView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)

        // Get the fish item data for this position
        val fishHistoryItem = historyList[position]

        // Populate views with data from the fish item
        fishNameTextView.text = fishHistoryItem.fishName
        fishImageView.setImageResource(fishHistoryItem.fishImageResId)
        timestampTextView.text = fishHistoryItem.timestamp

        // Handle deletion when delete button is clicked
        deleteButton.setOnClickListener {
            deleteItem(position)
        }

        // Open detailed fish history info when list item is clicked
        view.setOnClickListener {
            val intent = Intent(context, FishHistoryDescriptionActivity::class.java).apply {
                putExtra("fishName", fishHistoryItem.fishName)
                putExtra("fishImageResId", fishHistoryItem.fishImageResId)
                putExtra("timestamp", fishHistoryItem.timestamp)
            }
            context.startActivity(intent)
        }

        return view
    }
}
