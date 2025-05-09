package com.aquasnap.yolov8tflite

import android.os.Parcelable
import android.os.Parcel

// Data class to represent a history record of detected fish
data class FishHistoryItem(
    val fishName: String,         // Name of the detected fish
    val fishImageResId: Int,      // Resource ID of the fish image
    val timestamp: String         // Timestamp of when detection occurred
) : Parcelable {

    // Constructor to create the object from a Parcel
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",    // Read fish name (or default to empty if null)
        parcel.readInt(),             // Read image resource ID
        parcel.readString() ?: ""     // Read timestamp (or default to empty if null)
    )

    // Write object properties into the Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fishName)
        parcel.writeInt(fishImageResId)
        parcel.writeString(timestamp)
    }

    // Typically returns 0, used if object has special file descriptors (not in this case)
    override fun describeContents(): Int = 0

    // Parcelable.Creator is required to regenerate the object from a Parcel
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<FishHistoryItem> {
            override fun createFromParcel(parcel: Parcel): FishHistoryItem {
                return FishHistoryItem(parcel)
            }

            override fun newArray(size: Int): Array<FishHistoryItem?> {
                return arrayOfNulls(size)
            }
        }
    }
}
