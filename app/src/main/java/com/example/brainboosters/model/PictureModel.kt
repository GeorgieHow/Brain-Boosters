package com.example.brainboosters.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import java.util.Date


data class PictureModel(
    val imageUrl: String? = null,
    val imageName: String? = null,
    val documentId: String? = null,
    val imagePerson: String? = null,
    val imagePlace: String? = null,
    val imageEvent: String? = null,
    val imageDescription: String? = null,
    val imageYear: Int? = null,
    val timestamp: Timestamp? = null,


) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        timestamp = parcel.readLong().let {
            if (it == -1L) null else Timestamp(Date(it))
        }

    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imageUrl)
        parcel.writeString(imageName)
        parcel.writeString(documentId)
        parcel.writeString(imagePerson)
        parcel.writeString(imagePlace)
        parcel.writeString(imageEvent)
        parcel.writeString(imageDescription)
        if (imageYear != null) {
            parcel.writeInt(imageYear)
        }
        parcel.writeLong(timestamp?.toDate()?.time ?: -1L)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PictureModel> {
        val EMPTY = PictureModel(null, null, null, null, null, null, null, null)
        override fun createFromParcel(parcel: Parcel): PictureModel {
            return PictureModel(parcel)
        }

        override fun newArray(size: Int): Array<PictureModel?> {
            return arrayOfNulls(size)
        }
    }


}
