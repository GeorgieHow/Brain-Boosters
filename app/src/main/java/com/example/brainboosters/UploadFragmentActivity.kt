package com.example.brainboosters

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class UploadFragmentActivity : Fragment() {

    private val PICK_IMAGE_REQUEST = 1
    private lateinit var choosePictureButton: Button
    private lateinit var uploadButton: Button
    private lateinit var editFileName: EditText
    private lateinit var pictureImageView: ImageView
    private lateinit var imageUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.upload_fragment, container, false).apply {

        choosePictureButton = findViewById(R.id.choose_picture_button)
        uploadButton = findViewById(R.id.upload_button)
        editFileName = findViewById(R.id.photo_name_edit_text)
        pictureImageView = findViewById(R.id.chosen_picture_image_view)

        choosePictureButton.setOnClickListener {
            openPictureFolder()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<Button>(R.id.back_button)
        val galleryFragment = GalleryFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(galleryFragment)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the result here, e.g., get the selected image URI
            val imageUri = result.data?.data
            pictureImageView.setImageURI(imageUri)
        }
    }

    private fun openPictureFolder() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }
}