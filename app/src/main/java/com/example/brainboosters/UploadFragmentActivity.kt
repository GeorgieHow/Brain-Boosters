package com.example.brainboosters

import android.app.Activity
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class UploadFragmentActivity : Fragment() {

    private lateinit var choosePictureButton: Button
    private lateinit var uploadButton: Button
    private lateinit var editFileName: TextInputEditText
    private lateinit var pictureImageView: ImageView
    private lateinit var imageUri: Uri
    private lateinit var quizPictureButton: MaterialButton
    private lateinit var familyAlbumPictureButton: MaterialButton
    private lateinit var informationContainer: MaterialCardView
    private lateinit var quizInformationLayout: LinearLayout
    private lateinit var familyAlbumInformationLayout: LinearLayout

    private val selectedTags = HashSet<String>()

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val galleryFragment = GalleryFragmentActivity()
    private val uploadPart2Fragment = UploadFragmentPart2Activity()

    private var isQuizPhoto = false
    private var isFamilyAlbumPhoto = false

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

        quizPictureButton = findViewById(R.id.quiz_picture_button)
        familyAlbumPictureButton = findViewById(R.id.family_album_picture_button)

        informationContainer = findViewById(R.id.information_container)

        quizInformationLayout = findViewById(R.id.layout_for_quiz_inputs)
        familyAlbumInformationLayout = findViewById(R.id.layout_for_family_album)

        uploadButton.visibility = View.GONE
        quizPictureButton.visibility = View.GONE
        familyAlbumPictureButton.visibility = View.GONE
        informationContainer.visibility = View.GONE
        quizInformationLayout.visibility = View.GONE
        familyAlbumInformationLayout.visibility = View.GONE

        quizPictureButton.setBackgroundColor(Color.parseColor("#38656B"))
        familyAlbumPictureButton.setBackgroundColor(Color.parseColor("#917C9F"))
        informationContainer.setCardBackgroundColor(Color.parseColor("#c8ebfa"))

        quizPictureButton.setOnClickListener {
            informationContainer.setCardBackgroundColor(Color.parseColor("#c8ebfa"))
            quizInformationLayout.visibility = View.VISIBLE
            familyAlbumInformationLayout.visibility = View.GONE
            informationContainer.visibility = View.VISIBLE
            uploadButton.visibility = View.VISIBLE

            isQuizPhoto = true
            isFamilyAlbumPhoto = false
        }

        familyAlbumPictureButton.setOnClickListener {
            informationContainer.setCardBackgroundColor(Color.parseColor("#E6E6FA"))
            quizInformationLayout.visibility = View.GONE
            familyAlbumInformationLayout.visibility = View.VISIBLE
            informationContainer.visibility = View.VISIBLE
            uploadButton.visibility = View.VISIBLE

            isQuizPhoto = false
            isFamilyAlbumPhoto = true
        }

        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.tags_autocomplete_edit_text)
        val chipGroup = findViewById<ChipGroup>(R.id.chip_group)

        fetchTags { tags ->
            // Once tags are fetched, set up the adapter
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tags)
            autoCompleteTextView.setAdapter(adapter)
        }

        // Listen for completion and add tags as Chips
        autoCompleteTextView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tagName = autoCompleteTextView.text.toString()
                if (tagName.isNotEmpty()) {
                    addTagAsChip(tagName, chipGroup)
                    autoCompleteTextView.text = null // Clear input
                }
                true
            } else {
                false
            }
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

    private fun fetchTags(callback: (List<String>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserUid = mAuth.currentUser?.uid

        firestore.collection("tags")
            .whereEqualTo("uid", currentUserUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val tags = snapshot.documents.mapNotNull { it.getString("tagName") }.toList()
                callback(tags)
            }.addOnFailureListener {
                // Handle any errors
                callback(emptyList())
            }
    }

    private fun addTagAsChip(tagName: String, chipGroup: ChipGroup) {
        if (selectedTags.add(tagName)) { // Adds the tag if it's not already present
            val chip = Chip(context).apply {
                text = tagName
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    chipGroup.removeView(this)
                    selectedTags.remove(tagName) // Remove tag from selectedTags when chip is removed
                }
            }
            chipGroup.addView(chip)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the result here, e.g., get the selected image URI
            imageUri = result.data?.data!!
            pictureImageView.setImageURI(imageUri)

            quizPictureButton.visibility = View.VISIBLE
            familyAlbumPictureButton.visibility = View.VISIBLE
        }
    }

    private fun openPictureFolder() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun uploadImage(uri: Uri) {
        if(isQuizPhoto && !isFamilyAlbumPhoto){
            val photoType = "quiz"

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
                                "event" to photoEvent,
                                "photoType" to photoType,
                                "createdAt" to FieldValue.serverTimestamp()
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
        else if(isFamilyAlbumPhoto && !isQuizPhoto){
            val photoType = "family album"

            val photoDescriptionEditText = view?.findViewById<EditText>(R.id.photo_description_edit_text)
            val photoDescription = photoDescriptionEditText?.text.toString()

            val tagCollection = db.collection("tags")

            val tags = selectedTags.toList()
            val uid = mAuth.currentUser?.uid

            if (uri != null){
                val storageReference = storage.reference

                val fileName = UUID.randomUUID().toString() + " .jpg"
                val imagesRef = storageReference.child("images/$fileName")

                tags.forEach { tagName ->
                    tagCollection.whereEqualTo("tagName", tagName).get().addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            tagCollection.add(mapOf("tagName" to tagName, "uid" to uid))
                        }
                    }
                }

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
                                "uid" to uid,
                                "imageUrl" to imageUrl,
                                "description" to photoDescription,
                                "tags" to tags,
                                "photoType" to photoType,
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            val imagesCollection = db.collection("images")
                            imagesCollection.add(imageDetails)
                                .addOnSuccessListener {
                                    (activity as HomePageActivity).changeFragment(galleryFragment)
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
}