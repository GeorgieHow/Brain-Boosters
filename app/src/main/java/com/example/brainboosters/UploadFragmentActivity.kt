package com.example.brainboosters

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class UploadFragmentActivity : Fragment() {

    private lateinit var choosePictureButton: Button
    private lateinit var uploadButton: Button
    private lateinit var editFileName: TextInputEditText
    private lateinit var pictureImageView: ImageView
    private lateinit var imageUri: Uri

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val galleryFragment = GalleryFragmentActivity()
    private val uploadPart2Fragment = UploadFragmentPart2Activity()

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
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(galleryFragment)
        }

        val uploadButton = view.findViewById<Button>(R.id.upload_button)
        uploadButton.setOnClickListener {
            if(this::imageUri.isInitialized){
                uploadImage(imageUri)
            }
            else{
                Toast.makeText(context, "Please select a picture before uploading.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the result here, e.g., get the selected image URI
            imageUri = result.data?.data!!
            pictureImageView.setImageURI(imageUri)
        }
    }

    private fun openPictureFolder() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun uploadImage(uri: Uri) {
        val photoNameEditText = view?.findViewById<EditText>(R.id.photo_name_edit_text)
        val photoName = photoNameEditText?.text.toString()

        val photoYearEditText = view?.findViewById<EditText>(R.id.photo_year_edit_text)
        val photoYear = photoYearEditText?.text.toString()

        val photoWhereEditText = view?.findViewById<EditText>(R.id.photo_where_edit_text)
        val photoWhere = photoWhereEditText?.text.toString()

        val photoWhoEditText = view?.findViewById<EditText>(R.id.photo_who_edit_text)
        val photoWho = photoWhoEditText?.text.toString()

        val photoEventEditText = view?.findViewById<EditText>(R.id.photo_event_edit_text)
        val photoEvent = photoEventEditText?.text.toString()

        if (photoName.isEmpty() || photoWhere.isEmpty() || photoEvent.isEmpty()) {
            Toast.makeText(context, "All text fields with a * on the end must be filled in.",
                Toast.LENGTH_SHORT).show()
            return // Stop the function if validation fails
        }

        // Check if photoYear is a number and exactly four digits long
        val photoYearNum: Int? = try {
            photoYear.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Photo year must be a valid four-digit number", Toast.LENGTH_SHORT).show()
            return
        }

        // Additional check for the year being exactly four digits, if necessary
        if (photoYearNum != null && photoYear.length != 4) {
            Toast.makeText(context, "Photo year must be exactly four digits long", Toast.LENGTH_SHORT).show()
            return
        }

        if (uri != null){
            val storageReference = storage.reference

            val fileName = UUID.randomUUID().toString() + " .jpg"
            val imagesRef = storageReference.child("images/$fileName")

            imagesRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    // Now, get the download URL
                    imagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val imageUrl = downloadUri.toString()

                        // Store the download URL in Firestore or perform other actions
                        val uid = mAuth.currentUser?.uid
                        val uriString: String = uri.toString()

                        val imageDetails = hashMapOf(
                            "name" to photoName,
                            "uid" to uid,
                            "imageUrl" to imageUrl,
                            "year" to photoYearNum,
                            "place" to photoWhere,
                            "person" to photoWho,
                            "event" to photoEvent
                        )

                        val imagesCollection = db.collection("images")
                        imagesCollection.add(imageDetails)
                            .addOnSuccessListener { documentReference ->
                                // Document added successfully, now get the document ID
                                val photoId = documentReference.id
                                Log.d("Firestore", "DocumentSnapshot added with ID: $photoId")

                                // Pass the photo ID to UploadFragmentPart2Activity
                                val bundle = Bundle().apply {
                                    putString("photoId", photoId)
                                }
                                uploadPart2Fragment.arguments = bundle

                                // Navigate to UploadFragmentPart2Activity
                                (activity as HomePageActivity).changeFragment(uploadPart2Fragment)
                            }
                            .addOnFailureListener { e ->
                                // Handle the case where adding the document fails
                                Log.e("Firestore", "Error adding document", e)
                                Toast.makeText(context, "Failed to upload image details.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle unsuccessful upload
                }


        }
        else {
            Log.d("Firestore", "Cant ;P")
        }
    }
}