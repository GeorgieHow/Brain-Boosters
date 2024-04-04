package com.example.brainboosters

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class PictureFragmentActivity : Fragment() {

    private val typeOfPicture = ""

    val mAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    private val galleryFragment = GalleryFragmentActivity()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.view_picture_layout, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<Button>(R.id.back_button)
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(galleryFragment)
        }

        // Access the arguments to get the image details
        val imageId = arguments?.getString(ARG_IMAGE_ID)
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
        val imageName = arguments?.getString(ARG_IMAGE_NAME)
        val imageYear = arguments?.getString(ARG_IMAGE_YEAR)
        val imagePlace = arguments?.getString(ARG_IMAGE_PLACE)
        val imageEvent = arguments?.getString(ARG_EVENT)
        val imageDescription = arguments?.getString(ARG_DESCRIPTION)
        val imagePerson = arguments?.getString(ARG_PERSON)
        val imagePriority = arguments?.getString(ARG_PRIORITY)
        val imageTags = arguments?.getStringArrayList(ARG_TAGS)
        val imageCreation = arguments?.getString(ARG_CREATED_AT)
        val imageType = arguments?.getString(ARG_IMAGE_TYPE)

        // Use imageUrl and imageName to display details in your fragment
        val fileNameTextView: TextView = view.findViewById(R.id.file_name_text)
        fileNameTextView.text = imageName
        val fileNameTitleTextView: TextView = view.findViewById(R.id.file_name_title)
        val fileNameEditText: EditText = view.findViewById(R.id.file_name_edit_text)
        fileNameEditText.setText(imageName)

        val fileYearTextView: TextView = view.findViewById(R.id.file_year_text)
        fileYearTextView.text = imageYear
        val fileYearTitleTextView: TextView = view.findViewById(R.id.file_year_title)
        val fileYearEditText: EditText = view.findViewById(R.id.file_year_edit_text)
        fileYearEditText.setText(imageYear)

        val filePlaceTextView: TextView = view.findViewById(R.id.file_place_text)
        filePlaceTextView.text = imagePlace
        val filePlaceTitleTextView: TextView = view.findViewById(R.id.file_place_title)
        val filePlaceEditText: EditText = view.findViewById(R.id.file_place_edit_text)
        filePlaceEditText.setText(imagePlace)

        val fileEventTextView: TextView = view.findViewById(R.id.event_text)
        fileEventTextView.text = imageEvent
        val fileEventTitleTextView: TextView = view.findViewById(R.id.event_title)
        val fileEventEditText: EditText = view.findViewById(R.id.file_event_edit_text)
        fileEventEditText.setText(imageEvent)

        val fileDescriptionTextView: TextView = view.findViewById(R.id.description_text)
        fileDescriptionTextView.text = imageDescription
        val fileDescriptionEditText: EditText = view.findViewById(R.id.file_description_edit_text)
        fileDescriptionEditText.setText(imageDescription)

        val filePersonTextView: TextView = view.findViewById(R.id.person_text)
        filePersonTextView.text = imagePerson
        val filePersonTitleTextView: TextView = view.findViewById(R.id.person_title)
        val filePersonEditText: EditText = view.findViewById(R.id.file_person_edit_text)
        filePersonEditText.setText(imagePerson)

        val filePriorityTextView: TextView = view.findViewById(R.id.priority_text)
        filePriorityTextView.text = imagePriority

        val fileTagsTextView: TextView = view.findViewById(R.id.tags_text)
        fileTagsTextView.text = imageTags.toString()
        val fileTagsEditText: AutoCompleteTextView = view.findViewById(R.id.file_tags_edit_text)
        val tagsChipGroup: ChipGroup = view.findViewById(R.id.tags_chip_group)

        val fileCreatedAtTextView: TextView = view.findViewById(R.id.created_at_text)
        fileCreatedAtTextView.text = imageCreation
        val fileCreatedTitle: TextView = view.findViewById(R.id.created_at_title)

        val prioritySpinner: Spinner = view.findViewById(R.id.file_priority_spinner)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.priority_levels,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            prioritySpinner.adapter = adapter
        }

        // Preselect the Spinner based on the `imagePriority` value
        val priorities = resources.getStringArray(R.array.priority_levels)
        imagePriority?.let {
            val position = priorities.indexOf(it)
            if (position >= 0) prioritySpinner.setSelection(position)
        }

        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.picture_constraint_layout)

        if(imageType == "family album"){
            fileNameTextView.visibility = View.GONE
            fileNameTitleTextView.visibility = View.GONE
            fileYearTextView.visibility = View.GONE
            fileYearTitleTextView.visibility = View.GONE
            filePlaceTextView.visibility = View.GONE
            filePlaceTitleTextView.visibility = View.GONE
            fileEventTextView.visibility = View.GONE
            fileEventTitleTextView.visibility = View.GONE
            filePersonTextView.visibility = View.GONE
            filePersonTitleTextView.visibility = View.GONE
        }

        val filePictureImageView: ImageView = view.findViewById(R.id.picture_image_view)
        Glide.with(filePictureImageView.context)
            .load(imageUrl)
            .into(filePictureImageView)

        val editButton: Button = view.findViewById(R.id.edit_button)
        val confirmEditButton: Button = view.findViewById(R.id.confirm_edit_button)
        val cancelEditButton: Button = view.findViewById(R.id.cancel_button)
        val deletePictureButton: Button = view.findViewById(R.id.delete_picture_button)

        deletePictureButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.errorColor))

        fileTagsEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tagName = fileTagsEditText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    // Add the chip to the ChipGroup
                    addChipToGroup(tagName, tagsChipGroup)
                    // Clear the input
                    fileTagsEditText.text = null
                    // Check and add the tag to Firebase if it doesn't exist
                    addTagToFirebaseIfNotExists(tagName)
                }
                true // Consumes the action
            } else {
                false // Doesn't consume the action
            }
        }

        editButton.setOnClickListener {
            if(imageType == "quiz"){

                backButton.visibility = View.GONE
                editButton.visibility = View.GONE
                confirmEditButton.visibility = View.VISIBLE
                cancelEditButton.visibility = View.VISIBLE
                deletePictureButton.visibility = View.VISIBLE

                fileNameTextView.visibility = View.GONE
                fileNameEditText.visibility = View.VISIBLE

                fileYearTextView.visibility = View.GONE
                fileYearEditText.visibility = View.VISIBLE

                filePlaceTextView.visibility = View.GONE
                filePlaceEditText.visibility = View.VISIBLE

                fileEventTextView.visibility = View.GONE
                fileEventEditText.visibility = View.VISIBLE

                fileDescriptionTextView.visibility = View.GONE
                fileDescriptionEditText.visibility = View.VISIBLE

                filePersonTextView.visibility = View.GONE
                filePersonEditText.visibility = View.VISIBLE

                filePriorityTextView.visibility = View.GONE
                prioritySpinner.visibility = View.VISIBLE

                fileTagsTextView.visibility = View.GONE
                fileTagsEditText.visibility = View.VISIBLE

                filePictureImageView.visibility = View.GONE
                fileCreatedAtTextView.visibility = View.GONE
                fileCreatedTitle.visibility = View.GONE

                tagsChipGroup.visibility = View.VISIBLE

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)

                constraintSet.connect(filePersonTitleTextView.id, ConstraintSet.TOP, fileDescriptionEditText.id, ConstraintSet.BOTTOM, 10)

                constraintSet.applyTo(constraintLayout)

                // Fetch tags for the logged-in user and populate the AutoCompleteTextView
                val userId = mAuth.currentUser?.uid
                if (userId != null) {
                    db.collection("tags")
                        .whereEqualTo("uid", userId)
                        .get()
                        .addOnSuccessListener { documents ->
                            val tags = documents.mapNotNull { it.getString("tagName") }
                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tags)
                            fileTagsEditText.setAdapter(adapter)
                        }
                }

                // Load existing tags as chips
                loadExistingTagsAsChips(tagsChipGroup, imageTags)
            }else{
                backButton.visibility = View.GONE
                editButton.visibility = View.GONE
                confirmEditButton.visibility = View.VISIBLE
                cancelEditButton.visibility = View.VISIBLE
                deletePictureButton.visibility = View.VISIBLE

                fileDescriptionTextView.visibility = View.GONE
                fileDescriptionEditText.visibility = View.VISIBLE

                filePriorityTextView.visibility = View.GONE
                prioritySpinner.visibility = View.VISIBLE

                fileTagsTextView.visibility = View.GONE
                fileTagsEditText.visibility = View.VISIBLE

                filePictureImageView.visibility = View.GONE
                fileCreatedAtTextView.visibility = View.GONE
                fileCreatedTitle.visibility = View.GONE

                tagsChipGroup.visibility = View.VISIBLE

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                val priorityTitleTextView: TextView = view.findViewById(R.id.priority_title)

                constraintSet.connect(priorityTitleTextView.id, ConstraintSet.TOP, fileDescriptionEditText.id, ConstraintSet.BOTTOM, 10)
                constraintSet.applyTo(constraintLayout)

                // Fetch tags for the logged-in user and populate the AutoCompleteTextView
                val userId = mAuth.currentUser?.uid
                if (userId != null) {
                    db.collection("tags")
                        .whereEqualTo("uid", userId)
                        .get()
                        .addOnSuccessListener { documents ->
                            val tags = documents.mapNotNull { it.getString("tagName") }
                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tags)
                            fileTagsEditText.setAdapter(adapter)
                        }
                }

                // Load existing tags as chips
                loadExistingTagsAsChips(tagsChipGroup, imageTags)
            }
        }

        confirmEditButton.setOnClickListener {
            if(imageType == "quiz") {
                val imageName = fileNameEditText.text.toString()
                val imageYear = fileYearEditText.text.toString().toIntOrNull()
                val imagePlace = filePlaceEditText.text.toString()
                val imageEvent = fileEventEditText.text.toString()
                val imageDescription = fileDescriptionEditText.text.toString()
                val imagePerson = filePersonEditText.text.toString()
                val imagePriority = prioritySpinner.selectedItem.toString()

                // Collect tags from ChipGroup
                val tags = mutableListOf<String>()
                for (i in 0 until tagsChipGroup.childCount) {
                    val chip = tagsChipGroup.getChildAt(i) as Chip
                    tags.add(chip.text.toString())
                }

                // Assume you have a path or unique identifier for the image/document you're updating
                val imageDocRef = imageId?.let { it1 -> db.collection("images").document(it1) }

                val updatedImageDetails = hashMapOf(
                    "name" to imageName,
                    "year" to imageYear,
                    "place" to imagePlace,
                    "event" to imageEvent,
                    "description" to imageDescription,
                    "person" to imagePerson,
                    "priority" to imagePriority,
                    "tags" to tags
                )

                if (imageDocRef != null) {
                    imageDocRef.update(updatedImageDetails as Map<String, Any>)
                        .addOnSuccessListener {
                            // Handle success
                            Toast.makeText(
                                context,
                                "Image details updated successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Optionally, navigate the user away from the edit screen or refresh the data
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                            Toast.makeText(
                                context,
                                "Error updating image details: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                backButton.visibility = View.VISIBLE
                editButton.visibility = View.VISIBLE
                confirmEditButton.visibility = View.GONE
                cancelEditButton.visibility = View.GONE
                deletePictureButton.visibility = View.GONE

                fileNameTextView.text = imageName
                fileNameTextView.visibility = View.VISIBLE
                fileNameEditText.visibility = View.GONE

                fileYearTextView.text = imageYear.toString()
                fileYearTextView.visibility = View.VISIBLE
                fileYearEditText.visibility = View.GONE

                filePlaceTextView.text = imagePlace
                filePlaceTextView.visibility = View.VISIBLE
                filePlaceEditText.visibility = View.GONE

                fileEventTextView.text = imageEvent
                fileEventTextView.visibility = View.VISIBLE
                fileEventEditText.visibility = View.GONE

                fileDescriptionTextView.text = imageDescription
                fileDescriptionTextView.visibility = View.VISIBLE
                fileDescriptionEditText.visibility = View.GONE

                filePersonTextView.text = imagePerson
                filePersonTextView.visibility = View.VISIBLE
                filePersonEditText.visibility = View.GONE

                filePriorityTextView.text = imagePriority
                filePriorityTextView.visibility = View.VISIBLE
                prioritySpinner.visibility = View.GONE

                fileTagsTextView.text = tags.toString()
                fileTagsTextView.visibility = View.VISIBLE
                fileTagsEditText.visibility = View.GONE

                filePictureImageView.visibility = View.VISIBLE
                fileCreatedAtTextView.visibility = View.VISIBLE
                fileCreatedTitle.visibility = View.VISIBLE

                tagsChipGroup.visibility = View.GONE

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)

                constraintSet.connect(
                    filePersonTitleTextView.id,
                    ConstraintSet.TOP,
                    fileDescriptionTextView.id,
                    ConstraintSet.BOTTOM,
                    10
                )

                constraintSet.applyTo(constraintLayout)
            }
            else{
                val imageDescription = fileDescriptionEditText.text.toString()
                val imagePriority = prioritySpinner.selectedItem.toString()

                // Collect tags from ChipGroup
                val tags = mutableListOf<String>()
                for (i in 0 until tagsChipGroup.childCount) {
                    val chip = tagsChipGroup.getChildAt(i) as Chip
                    tags.add(chip.text.toString())
                }

                // Assume you have a path or unique identifier for the image/document you're updating
                val imageDocRef = imageId?.let { it1 -> db.collection("images").document(it1) }

                val updatedImageDetails = hashMapOf(
                    "description" to imageDescription,
                    "priority" to imagePriority,
                    "tags" to tags
                )

                if (imageDocRef != null) {
                    imageDocRef.update(updatedImageDetails as Map<String, Any>)
                        .addOnSuccessListener {
                            // Handle success
                            Toast.makeText(
                                context,
                                "Image details updated successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Optionally, navigate the user away from the edit screen or refresh the data
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                            Toast.makeText(
                                context,
                                "Error updating image details: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                backButton.visibility = View.VISIBLE
                editButton.visibility = View.VISIBLE
                confirmEditButton.visibility = View.GONE
                cancelEditButton.visibility = View.GONE
                deletePictureButton.visibility = View.GONE

                fileDescriptionTextView.text = imageDescription
                fileDescriptionTextView.visibility = View.VISIBLE
                fileDescriptionEditText.visibility = View.GONE

                filePriorityTextView.text = imagePriority
                filePriorityTextView.visibility = View.VISIBLE
                prioritySpinner.visibility = View.GONE

                fileTagsTextView.text = tags.toString()
                fileTagsTextView.visibility = View.VISIBLE
                fileTagsEditText.visibility = View.GONE

                filePictureImageView.visibility = View.VISIBLE
                fileCreatedAtTextView.visibility = View.VISIBLE
                fileCreatedTitle.visibility = View.VISIBLE

                tagsChipGroup.visibility = View.GONE

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                val priorityTitleTextView: TextView = view.findViewById(R.id.priority_title)

                constraintSet.connect(priorityTitleTextView.id, ConstraintSet.TOP, fileDescriptionTextView.id, ConstraintSet.BOTTOM, 10)
                constraintSet.applyTo(constraintLayout)
            }
        }

        deletePictureButton.setOnClickListener {
            val imageId = arguments?.getString(ARG_IMAGE_ID) ?: return@setOnClickListener

            // Start by deleting the picture from the 'images' collection
            db.collection("images").document(imageId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Image successfully deleted", Toast.LENGTH_SHORT).show()

                    // Now find related quizzes in 'quizImageLinks' using the imageId
                    db.collection("quizImageLinks")
                        .whereEqualTo("imageId", imageId)
                        .get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                // For each quizImageLink, get the quizId and delete the related quiz
                                val quizId = document.getString("quizId") ?: continue
                                db.collection("quizzes").document(quizId)
                                    .delete()
                                    .addOnSuccessListener {
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("DeleteQuiz", "Error deleting related quiz", e)
                                    }

                                // Also delete the quizImageLink document itself
                                db.collection("quizImageLinks").document(document.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        // Successfully deleted quizImageLink document
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("DeleteLink", "Error deleting quiz image link", e)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FindQuizLinks", "Error finding quiz image links", e)
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error deleting image: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            (activity as HomePageActivity).changeFragment(galleryFragment)
        }

        cancelEditButton.setOnClickListener {
            if(imageType == "quiz") {

                backButton.visibility = View.VISIBLE
                editButton.visibility = View.VISIBLE
                confirmEditButton.visibility = View.GONE
                cancelEditButton.visibility = View.GONE
                deletePictureButton.visibility = View.GONE

                fileNameTextView.visibility = View.VISIBLE
                fileNameEditText.visibility = View.GONE

                fileYearTextView.visibility = View.VISIBLE
                fileYearEditText.visibility = View.GONE

                filePlaceTextView.visibility = View.VISIBLE
                filePlaceEditText.visibility = View.GONE

                fileEventTextView.visibility = View.VISIBLE
                fileEventEditText.visibility = View.GONE

                fileDescriptionTextView.visibility = View.VISIBLE
                fileDescriptionEditText.visibility = View.GONE

                filePersonTextView.visibility = View.VISIBLE
                filePersonEditText.visibility = View.GONE

                filePriorityTextView.visibility = View.VISIBLE
                prioritySpinner.visibility = View.GONE

                fileTagsTextView.visibility = View.VISIBLE
                fileTagsEditText.visibility = View.GONE

                filePictureImageView.visibility = View.VISIBLE
                fileCreatedAtTextView.visibility = View.VISIBLE
                fileCreatedTitle.visibility = View.VISIBLE

                tagsChipGroup.visibility = View.GONE

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)

                constraintSet.connect(filePersonTitleTextView.id, ConstraintSet.TOP, fileDescriptionTextView.id, ConstraintSet.BOTTOM, 10)

                constraintSet.applyTo(constraintLayout)
            }
            else{
                backButton.visibility = View.VISIBLE
                editButton.visibility = View.VISIBLE
                confirmEditButton.visibility = View.GONE
                cancelEditButton.visibility = View.GONE
                deletePictureButton.visibility = View.GONE

                fileDescriptionTextView.visibility = View.VISIBLE
                fileDescriptionEditText.visibility = View.GONE

                filePriorityTextView.visibility = View.VISIBLE
                prioritySpinner.visibility = View.GONE

                fileTagsTextView.visibility = View.VISIBLE
                fileTagsEditText.visibility = View.GONE

                filePictureImageView.visibility = View.VISIBLE
                fileCreatedAtTextView.visibility = View.VISIBLE
                fileCreatedTitle.visibility = View.VISIBLE

                tagsChipGroup.visibility = View.GONE

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                val priorityTitleTextView: TextView = view.findViewById(R.id.priority_title)

                constraintSet.connect(priorityTitleTextView.id, ConstraintSet.TOP, fileDescriptionTextView.id, ConstraintSet.BOTTOM, 10)
                constraintSet.applyTo(constraintLayout)
            }
        }


    }

    private fun loadExistingTagsAsChips(chipGroup: ChipGroup, tags: ArrayList<String>?) {
        chipGroup.removeAllViews()
        tags?.forEach { tag ->
            addChipToGroup(tag, chipGroup)
        }
    }

    private fun addChipToGroup(tag: String, chipGroup: ChipGroup) {
        val chip = Chip(context).apply {
            text = tag
            isCloseIconVisible = true
            setOnCloseIconClickListener { chipGroup.removeView(this) }
        }
        chipGroup.addView(chip)
    }

    private fun addTagToFirebaseIfNotExists(tagName: String) {
        val userId = mAuth.currentUser?.uid ?: return // Return early if user ID is null
        // Query to check if the tag already exists
        db.collection("tags")
            .whereEqualTo("uid", userId)
            .whereEqualTo("tagName", tagName)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // The tag doesn't exist, add it
                    val newTag = hashMapOf(
                        "uid" to userId,
                        "tagName" to tagName
                    )
                    db.collection("tags")
                        .add(newTag)
                        .addOnSuccessListener {
                            // Successfully added new tag
                            Toast.makeText(context, "New tag added: $tagName", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error adding tag: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    companion object {
        private const val ARG_IMAGE_ID = "image_id"
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_IMAGE_NAME = "image_name"
        private const val ARG_IMAGE_YEAR = "image_year"
        private const val ARG_IMAGE_PLACE = "image_place"

        private const val ARG_EVENT = "event"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_PERSON = "person"
        private const val ARG_PRIORITY = "priority"
        private const val ARG_TAGS = "tags"

        private const val ARG_CREATED_AT = "created_at"

        private const val  ARG_IMAGE_TYPE = "image_type"

        fun newInstance(imageId: String, imageUrl: String, imageName: String, imageYear: String, imagePlace: String?,
                        event: String? = null, description: String? = null, person: String? = null,
                        priority: String? = null, tags: ArrayList<String>? = null, createdAt: String? = null, type: String? = null):
                PictureFragmentActivity {
            val fragment = PictureFragmentActivity()
            val args = Bundle().apply {
                putString(ARG_IMAGE_ID, imageId)
                putString(ARG_IMAGE_URL, imageUrl)
                putString(ARG_IMAGE_NAME, imageName)
                putString(ARG_IMAGE_YEAR, imageYear)
                putString(ARG_IMAGE_PLACE, imagePlace)

                event?.let { putString(ARG_EVENT, it) }
                description?.let { putString(ARG_DESCRIPTION, it) }
                person?.let { putString(ARG_PERSON, it) }
                priority?.let { putString(ARG_PRIORITY, it) }
                putStringArrayList(ARG_TAGS, tags)
                createdAt?.let { putString(ARG_CREATED_AT, it) }

                putString(ARG_IMAGE_TYPE, type)
            }
            fragment.arguments = args
            return fragment
        }
    }
}