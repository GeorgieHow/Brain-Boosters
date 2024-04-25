package com.example.brainboosters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A Fragment class to display information about a selected picture from the Family Album when
 * its clicked on.
 */
class FamilyAlbumPictureActivity: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.family_album_photo_fragment, container, false).apply {

        // Retrieve image details passed from another fragment or activity via the companion object
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
        val imageId = arguments?.getString(ARG_IMAGE_ID)
        val imageDescription = arguments?.getString(ARG_IMAGE_DESCRIPTION)

        // Gets as string so it can be formatted in a readable way.
        val imageTimestampString = arguments?.getString(ARG_IMAGE_TIMESTAMP)
        val imageTimestamp = imageTimestampString?.let { formatFirebaseTimestamp(parseStringToTimestamp(it)) }

        // Sets description and timestamp to the right text views.
        val timeStampTextView = findViewById<TextView>(R.id.timestamp_text_view)
        timeStampTextView.text = imageTimestamp.toString()
        val descriptionTextView = findViewById<TextView>(R.id.description_text_view)
        descriptionTextView.text = imageDescription

        // Loads picture into the ImageView via Glide.
        val pictureImageView = findViewById<ImageView>(R.id.picture_image_view)
        Glide.with(pictureImageView.context)
            .load(imageUrl)
            .into(pictureImageView)

        // Defines back button so user can navigate back to the family album fragment.
        val backButton = findViewById<Button>(R.id.back_button)
        val familyAlbumPage = FamilyAlbumActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(familyAlbumPage)
        }


    }

    /**
     * Parses a formatted timestamp string into timestamp object.
     */
    private fun parseStringToTimestamp(timestampString: String): Timestamp {
        val regex = "Timestamp\\(seconds=(\\d+), nanoseconds=(\\d+)\\)".toRegex()
        val matchResult = regex.find(timestampString)
        val (seconds, nanoseconds) = matchResult?.destructured
            ?: throw IllegalArgumentException("Invalid Timestamp format")

        return Timestamp(seconds.toLong(), nanoseconds.toInt())
    }

    /**
     * Formats the timestamp so it can be read easier by the user.
     */
    private fun formatFirebaseTimestamp(timestamp: Timestamp): String {
        val date = timestamp.toDate() // Convert Firebase Timestamp to Date

        val dayFormat = SimpleDateFormat("d", Locale.getDefault())
        val monthAndTimeFormat = SimpleDateFormat("MMMM h:mm", Locale.getDefault())

        val day = dayFormat.format(date).toInt()
        val dayWithOrdinal = getDayWithOrdinal(day)
        val monthAndTime = monthAndTimeFormat.format(date)

        return "$dayWithOrdinal $monthAndTime"
    }

    /**
     * Adds the right suffix on to the end of the day.
     */
    private fun getDayWithOrdinal(day: Int): String {
        val suffix = when (day % 10) {
            1 -> if (day % 100 == 11) "th" else "st"
            2 -> if (day % 100 == 12) "th" else "nd"
            3 -> if (day % 100 == 13) "th" else "rd"
            else -> "th"
        }
        return "$day$suffix"
    }

    /**
     * Companion object for parsing through details of picture from previous fragment.
     */
    companion object {

        // Constants defined to store the details parse through.
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_IMAGE_ID = "picture_id"
        private const val ARG_IMAGE_DESCRIPTION = "image_description"
        private const val ARG_IMAGE_TIMESTAMP = "image_createdAt"

        /**
         * Creates a new instance of this fragment with picture details gotten from previous
         * fragment.
         */
        fun newInstance(imageUrl: String, pictureId: String, imageDescription: String, imageTimestamp: String):
                FamilyAlbumPictureActivity {
            val fragment = FamilyAlbumPictureActivity()
            val args = Bundle()

            // Puts them all into the constant variables so they can be accessed in the fragment.
            args.putString(ARG_IMAGE_URL, imageUrl)
            args.putString(ARG_IMAGE_ID, pictureId)
            args.putString(ARG_IMAGE_DESCRIPTION, imageDescription)
            args.putString(ARG_IMAGE_TIMESTAMP, imageTimestamp)
            fragment.arguments = args
            return fragment
        }
    }
}