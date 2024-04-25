package com.example.brainboosters

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * A fragment which shows and lets a user edit their profile details.
 */
class ProfileFragmentActivity : Fragment() {

    // Initialises variables for profile picture.
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var originalProfilePicUrl: String? = null

    // Gets instances of database and authentication to get user details.
    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profileImageView: ShapeableImageView = view.findViewById(R.id.profile_image_view)

        // Fetches user details from firebase with method.
        mAuth.currentUser?.let { fetchUserDetailsFromFirebase(it.uid) }

        // Loads image launcher so when user wants to change image they can pick from device.
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                selectedImageUri = result.data?.data

                // Loads the picture into the profile picture image view.
                Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.profile_picture))
                    .circleCrop()
                    .into(profileImageView)
            }
        }

        // Gets all buttons needed for this page.
        val editProfileButton: Button = view.findViewById(R.id.edit_profile_button)
        val cancelEditProfileButton: Button = view.findViewById(R.id.cancel_edit_profile_button)
        val confirmEditProfileButton: Button = view.findViewById(R.id.confirm_edit_profile_button)
        val editPhotoButton: Button = view.findViewById(R.id.edit_picture_button)
        cancelEditProfileButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.errorColor))

        // When clicked, launches devices image file.
        editPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        val fullNameText: TextView = view.findViewById(R.id.full_name_text)
        val ageText: TextView = view.findViewById(R.id.age_text)
        val dementiaTypeText: TextView = view.findViewById(R.id.dementia_type_text)
        val dementiaLevelText: TextView = view.findViewById(R.id.dementia_level_text)

        val editFullNameEditText: EditText = view.findViewById(R.id.full_name_edit_text)
        val editAgeEditText: EditText = view.findViewById(R.id.age_edit_text)
        val editDementiaTypeEditText: EditText = view.findViewById(R.id.dementia_type_edit_text)
        val editDementiaLevelSpinner: Spinner = view.findViewById(R.id.dementia_level_spinner)

        // Sets up spinner for dementia level.
        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.dementia_levels,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        editDementiaLevelSpinner.adapter = adapter

        // Sets up edit button to show the right edit boxes.
        editProfileButton.setOnClickListener {
            editProfileButton.visibility = View.GONE
            cancelEditProfileButton.visibility = View.VISIBLE
            confirmEditProfileButton.visibility = View.VISIBLE
            editPhotoButton.visibility = View.VISIBLE

            fullNameText.visibility = View.GONE
            ageText.visibility = View.GONE
            dementiaTypeText.visibility = View.GONE
            dementiaLevelText.visibility = View.GONE

            editFullNameEditText.visibility = View.VISIBLE
            editAgeEditText.visibility = View.VISIBLE
            editDementiaTypeEditText.visibility = View.VISIBLE
            editDementiaLevelSpinner.visibility = View.VISIBLE
        }

        // Reverts the page back to text views if cancelled.
        cancelEditProfileButton.setOnClickListener {
            editProfileButton.visibility = View.VISIBLE
            cancelEditProfileButton.visibility = View.GONE
            confirmEditProfileButton.visibility = View.GONE
            editPhotoButton.visibility = View.GONE

            fullNameText.visibility = View.VISIBLE
            ageText.visibility = View.VISIBLE
            dementiaTypeText.visibility = View.VISIBLE
            dementiaLevelText.visibility = View.VISIBLE

            editFullNameEditText.visibility = View.GONE
            editAgeEditText.visibility = View.GONE
            editDementiaTypeEditText.visibility = View.GONE
            editDementiaLevelSpinner.visibility = View.GONE

            // Need to return to old profile picture.
            selectedImageUri = Uri.parse(originalProfilePicUrl ?: return@setOnClickListener)
            loadProfileImage(originalProfilePicUrl)
        }

        // Sets up confirm edited button update details.
        confirmEditProfileButton.setOnClickListener {

            // Gets all new details for profile.
            val fullName = editFullNameEditText.text.toString()
            val age = editAgeEditText.text.toString().toIntOrNull()
            val dementiaType = editDementiaTypeEditText.text.toString()
            val dementiaLevel = editDementiaLevelSpinner.selectedItem.toString()

            originalProfilePicUrl = selectedImageUri.toString()

            // Checks certain aspects are not null and updates profile using the method.
            if (fullName.isNotBlank() && age != null && dementiaType.isNotBlank()) {
                updateUserData(fullName, age, dementiaType, dementiaLevel)

                if (selectedImageUri != null){
                    selectedImageUri?.let { uri ->
                        mAuth.currentUser?.uid?.let { userId ->
                            uploadImageToFirebaseStorage(uri, userId)
                        } ?: Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()}
                }
                else{
                    Uri.parse(originalProfilePicUrl)?.let { uri ->
                        mAuth.currentUser?.uid?.let { userId ->
                            uploadImageToFirebaseStorage(uri, userId)
                        } ?: Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()}
                }
            } else {
            }
        }
    }

    /**
     * A method to get the users details using their UID.
     *
     * @param userId The users id.
     */
    private fun fetchUserDetailsFromFirebase(userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null) {

                // Finds all user details from document.
                val name = document.getString("fullName") ?: "N/A"
                val age = document.getLong("age")?.toInt()?.toString() ?: "N/A"
                val email = mAuth.currentUser?.email
                val dementiaType = document.getString("dementiaType") ?: "N/A"
                val dementiaLevel = document.getString("dementiaLevel") ?: "N/A"

                // Makes sure spinner is set to the right level.
                val editDementiaLevelSpinner: Spinner = view?.findViewById(R.id.dementia_level_spinner) ?: return@addOnSuccessListener
                val adapter = editDementiaLevelSpinner.adapter
                val position = (0 until adapter.count).firstOrNull {
                    adapter.getItem(it).toString().equals(dementiaLevel, ignoreCase = true)
                } ?: 0
                val imageUrl = document.getString("profileImage")
                originalProfilePicUrl = document.getString("profileImage")

                // Loads profile picture with the url linked to account.
                loadProfileImage(imageUrl)
                editDementiaLevelSpinner.setSelection(position)

                view?.findViewById<TextView>(R.id.full_name_text)?.text = name
                view?.findViewById<TextView>(R.id.age_text)?.text = age.toString()
                view?.findViewById<TextView>(R.id.email_text)?.text = email
                view?.findViewById<TextView>(R.id.dementia_type_text)?.text = dementiaType
                view?.findViewById<TextView>(R.id.dementia_level_text)?.text = dementiaLevel

                view?.findViewById<EditText>(R.id.full_name_edit_text)?.setText(name)
                view?.findViewById<EditText>(R.id.age_edit_text)?.setText(age)
                view?.findViewById<EditText>(R.id.dementia_type_edit_text)?.setText(dementiaType)


            } else {
                Log.d("ProfileFragment", "No such document")
            }
        }.addOnFailureListener { exception ->
            Log.d("ProfileFragment", "get failed with ", exception)
        }
    }

    /**
     * A method to update the users data in firebase.
     *
     * @param fullName Users full name.
     * @param age Users age.
     * @param dementiaType Users type of dementia.
     * @param dementiaLevel Users level of dementia.
     */
    private fun updateUserData(fullName: String, age: Int, dementiaType: String, dementiaLevel: String) {

        // Gets user id.
        val userId = mAuth.currentUser?.uid ?: return
        val email = mAuth.currentUser?.email

        // Creates a hash map for the necessary updates.
        val userUpdates = hashMapOf<String, Any>(
            "fullName" to fullName,
            "age" to age,
            "dementiaType" to dementiaType,
            "dementiaLevel" to dementiaLevel,
            "email" to email.toString()
        )

        // Searches database for that user and updates details.
        db.collection("users").document(userId)
            .set(userUpdates)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "DocumentSnapshot successfully updated!")
                revertUIAfterUpdate(fullName, age.toString(), dementiaType, dementiaLevel)
            }
            .addOnFailureListener { e ->
                Log.w("ProfileFragment", "Error updating document", e)
            }
    }

    /**
     * A method to revert the UI once update has been made, as well as make sure the fields have
     * been updated with the data.
     *
     * @param fullName Users new full name.
     * @param age Users new age.
     * @param dementiaType Users new type of dementia.
     * @param dementiaLevel Users new level of dementia.
     */
    private fun revertUIAfterUpdate(fullName: String, age: String, dementiaType: String, dementiaLevel: String) {
        view?.findViewById<Button>(R.id.edit_profile_button)?.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.cancel_edit_profile_button)?.visibility = View.GONE
        view?.findViewById<Button>(R.id.confirm_edit_profile_button)?.visibility = View.GONE
        view?.findViewById<Button>(R.id.edit_picture_button)?.visibility = View.GONE

        view?.findViewById<TextView>(R.id.full_name_text)?.apply {
            text = fullName
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.age_text)?.apply {
            text = age
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.dementia_type_text)?.apply {
            text = dementiaType
            visibility = View.VISIBLE
        }
        view?.findViewById<TextView>(R.id.dementia_level_text)?.apply {
            text = dementiaLevel
            visibility = View.VISIBLE
        }

        view?.findViewById<EditText>(R.id.full_name_edit_text)?.visibility = View.GONE
        view?.findViewById<EditText>(R.id.age_edit_text)?.visibility = View.GONE
        view?.findViewById<EditText>(R.id.dementia_type_edit_text)?.visibility = View.GONE
        view?.findViewById<Spinner>(R.id.dementia_level_spinner)?.visibility = View.GONE
    }


    /**
     * A method to upload the profile picture to firebase.
     *
     * @param imageUri The images Uri, so it can be stored in storage.
     * @param userId the users id.
     */
    private fun uploadImageToFirebaseStorage(imageUri: Uri, userId: String) {

        //Gets firebase storage and creates the picture with the users id as the name
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId.jpg")

        // Attempts to put the file into storage
        storageRef.putFile(imageUri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Updates database with URL for user.
                val downloadUri = task.result
                updateFirestoreWithImageUrl(userId, downloadUri.toString())
            } else {
            }
        }
    }

    /**
     * A method which updates the users document with the url to the image.
     *
     * @param imageUrl Url link to the image.
     * @param userId The users id.
     */
    private fun updateFirestoreWithImageUrl(userId: String, imageUrl: String) {
        val db = FirebaseFirestore.getInstance()

        // Finds the user to update their profile picture
        db.collection("users").document(userId).update("profileImage", imageUrl)
            .addOnSuccessListener {

                // Loads into profile picture if successful.
                val profileImageView: ShapeableImageView? = view?.findViewById(R.id.profile_image_view)
                profileImageView?.let { imageView ->
                    Glide.with(this)
                        .load(selectedImageUri)
                        .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.profile_picture))
                        .circleCrop()
                        .into(imageView)
                }

                Toast.makeText(context, "Profile image updated successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update profile image.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * A method to load the image into the profile picture image view.
     *
     * @param imageUrl Url link to the image.
     */
    private fun loadProfileImage(imageUrl: String?) {
        val profileImageView: ShapeableImageView = view?.findViewById(R.id.profile_image_view) ?: return
        Glide.with(this)
            .load(imageUrl)
            .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.profile_picture))
            .circleCrop()
            .into(profileImageView)
    }
}