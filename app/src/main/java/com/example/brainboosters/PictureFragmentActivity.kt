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


class PictureFragmentActivity : Fragment() {

    private val galleryFragment = GalleryFragmentActivity()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.view_picture_layout, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access the arguments to get the image details
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
        val imageName = arguments?.getString(ARG_IMAGE_NAME)
        val imageYear = arguments?.getString(ARG_IMAGE_YEAR)
        val imagePlace = arguments?.getString(ARG_IMAGE_PLACE)

        // Use imageUrl and imageName to display details in your fragment
        val fileNameTextextView: TextView = view.findViewById(R.id.file_name_text)
        fileNameTextextView.text = imageName

        val fileYearTextView: TextView = view.findViewById(R.id.file_year_text)
        fileYearTextView.text = imageYear

        val filePlaceTextView: TextView = view.findViewById(R.id.file_place_text)
        filePlaceTextView.text = imagePlace

        val filePictureImageView: ImageView = view.findViewById(R.id.picture_image_view)
        Glide.with(filePictureImageView.context)
            .load(imageUrl)
            .into(filePictureImageView)

        val backButton = view.findViewById<Button>(R.id.back_button)
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(galleryFragment)
        }

    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_IMAGE_NAME = "image_name"
        private const val ARG_IMAGE_YEAR = "image_year"
        private const val ARG_IMAGE_PLACE = "image_place"

        fun newInstance(imageUrl: String, imageName: String, imageYear: String, imagePlace: String?):
                PictureFragmentActivity {
            val fragment = PictureFragmentActivity()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageUrl)
            args.putString(ARG_IMAGE_NAME, imageName)
            args.putString(ARG_IMAGE_YEAR, imageYear)
            args.putString(ARG_IMAGE_PLACE, imagePlace)
            fragment.arguments = args
            return fragment
        }
    }
}