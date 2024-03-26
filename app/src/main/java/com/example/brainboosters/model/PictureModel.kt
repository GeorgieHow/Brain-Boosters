package com.example.brainboosters.model
import android.os.Parcel
import android.os.Parcelable

data class PictureModel(
    val imageUrl: String?,
    val imageName: String? = null,
    val documentId: String? = null,
    val imagePerson: String? = null,
    val imagePlace: String? = null,
    val imageEvent: String? = null,
    val imageDescription: String? = null,
    val imageYear: Int? = null

) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt()
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
