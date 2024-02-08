package com.example.brainboosters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class PictureFragmentActivity : Fragment() {

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

        // Use imageUrl and imageName to display details in your fragment
        val fileNameTextextView: TextView = view.findViewById(R.id.file_name_text)
        fileNameTextextView.text = imageName

        val filePictureImageView: ImageView = view.findViewById(R.id.picture_image_view)
        Glide.with(filePictureImageView.context)
            .load(imageUrl)
            .into(filePictureImageView)
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_IMAGE_NAME = "image_name"

        fun newInstance(imageUrl: String, imageName: String): PictureFragmentActivity {
            val fragment = PictureFragmentActivity()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageUrl)
            args.putString(ARG_IMAGE_NAME, imageName)
            fragment.arguments = args
            return fragment
        }
    }
}