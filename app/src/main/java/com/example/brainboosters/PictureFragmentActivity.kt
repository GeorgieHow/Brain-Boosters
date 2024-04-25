package com.example.brainboosters

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


/**
 * The picture fragment which displays picture details and allows users to edit them.
 */
class PictureFragmentActivity : Fragment() {

    // Gets authentication and database, as well as fragment to navigate back to.
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

        // Sets up back button to navigate back to gallery fragment.
        val backButton = view.findViewById<Button>(R.id.back_button)
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(galleryFragment)
        }

        // Uses companion object to parse through the photo details.
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

        // Sets all the text views to the right details.
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

        // Creates spinner for priority levels of the picture.
        val prioritySpinner: Spinner = view.findViewById(R.id.file_priority_spinner)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.priority_levels,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            prioritySpinner.adapter = adapter
        }

        // Preselect the spinner, so when user edits its already on the current priority.
        val priorities = resources.getStringArray(R.array.priority_levels)
        imagePriority?.let {
            val position = priorities.indexOf(it)
            if (position >= 0) prioritySpinner.setSelection(position)
        }

        // Finds constraint layout needed so xml can be edited here to make sure layout still
        // works.
        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.picture_constraint_layout)

        // Hides some details if not needed based on photo type.
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

        // Loads picture into ImageView with Glide.
        val filePictureImageView: ImageView = view.findViewById(R.id.picture_image_view)
        Glide.with(filePictureImageView.context)
            .load(imageUrl)
            .into(filePictureImageView)

        // Gets all the buttons needed from layout.
        val editButton: Button = view.findViewById(R.id.edit_button)
        val confirmEditButton: Button = view.findViewById(R.id.confirm_edit_button)
        val cancelEditButton: Button = view.findViewById(R.id.cancel_button)
        val deletePictureButton: Button = view.findViewById(R.id.delete_picture_button)
        deletePictureButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.errorColor))

        // Used for adding tags to picture.
        fileTagsEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tagName = fileTagsEditText.text.toString().trim()
                if (tagName.isNotEmpty()) {

                    // Adds tag to chip group to show tag and resets auto fill text box.
                    addChipToGroup(tagName, tagsChipGroup)
                    fileTagsEditText.text = null

                    // Adds it to firebase if it doesnt already exist there.
                    addTagToFirebaseIfNotExists(tagName)
                }
                true
            } else {
                false
            }
        }

        // Sets up edit button so all text views change to edit texts.
        editButton.setOnClickListener {
            // For quiz pictures.
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

                // Fetch tags for the logged-in user and populate the AutoCompleteTextView.
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

                // Load existing tags as chips.
                loadExistingTagsAsChips(tagsChipGroup, imageTags)
            }else{
                // Family album pictyres have less details so are loaded differently.
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

                // Fetch tags for the logged-in user and populate the AutoCompleteTextView.
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

                // Load existing tags as chips.
                loadExistingTagsAsChips(tagsChipGroup, imageTags)
            }
        }

        // Sets up confirm edit button.
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

                // Gets image it wants to update.
                val imageDocRef = imageId?.let { it1 -> db.collection("images").document(it1) }

                // Creates hashmap of updated details.
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

                // If it can find the image, update all the values with the hashmap made.
                if (imageDocRef != null) {
                    imageDocRef.update(updatedImageDetails as Map<String, Any>)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { e ->
                            // In case photo cannot be updated, shows message.
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

                // Collect tags from ChipGroup.
                val tags = mutableListOf<String>()
                for (i in 0 until tagsChipGroup.childCount) {
                    val chip = tagsChipGroup.getChildAt(i) as Chip
                    tags.add(chip.text.toString())
                }

                val imageDocRef = imageId?.let { it1 -> db.collection("images").document(it1) }

                val updatedImageDetails = hashMapOf(
                    "description" to imageDescription,
                    "priority" to imagePriority,
                    "tags" to tags
                )

                if (imageDocRef != null) {
                    imageDocRef.update(updatedImageDetails as Map<String, Any>)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { e ->
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

        // Sets up delete button.
        deletePictureButton.setOnClickListener {
            val imageId = arguments?.getString(ARG_IMAGE_ID) ?: return@setOnClickListener

            // Finds image and deletes it from database.
            db.collection("images").document(imageId)
                .delete()
                .addOnSuccessListener {

                    // Looks through quiz image links with the id to delete those records as well.
                    db.collection("quizImageLinks")
                        .whereEqualTo("imageId", imageId)
                        .get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {

                                // For each quizImageLink, get the quizId and delete the related quiz.
                                val quizId = document.getString("quizId") ?: continue
                                db.collection("quizzes").document(quizId)
                                    .delete()
                                    .addOnSuccessListener {
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("DeleteQuiz", "Error deleting related quiz", e)
                                    }

                                // Deletes the quizImageLink document itself.
                                db.collection("quizImageLinks").document(document.id)
                                    .delete()
                                    .addOnSuccessListener {
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

            // Goes back to gallery fragment.
            (activity as HomePageActivity).changeFragment(galleryFragment)
        }

        // Sets up cancel edit button.
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

    /**
     * Loads all previous tags into the chip group.
     *
     * @param chipGroup The chip group.
     * @param tags The list of tag strings to be added to the chip group.
     */
    private fun loadExistingTagsAsChips(chipGroup: ChipGroup, tags: ArrayList<String>?) {
        // Clears chip group.
        chipGroup.removeAllViews()
        tags?.forEach { tag ->
            addChipToGroup(tag, chipGroup)
        }
    }

    /**
     * Adds a single tag as a chip to a chip group.
     *
     * @param tag The tag string to be added as a chip.
     * @param chipGroup The chip group.
     */
    private fun addChipToGroup(tag: String, chipGroup: ChipGroup) {
        val chip = Chip(context).apply {
            text = tag
            isCloseIconVisible = true
            // Removes chip if the 'X' is clicked.
            setOnCloseIconClickListener { chipGroup.removeView(this) }
        }

        // Adds to group.
        chipGroup.addView(chip)
    }

    /**
     * Adds tag to database if it does not already exist.
     *
     * @param tagName The name of the tag to check if its already there or add it.
     */
    private fun addTagToFirebaseIfNotExists(tagName: String) {
        val userId = mAuth.currentUser?.uid ?: return

        // Queries the tag for the user.
        db.collection("tags")
            .whereEqualTo("uid", userId)
            .whereEqualTo("tagName", tagName)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                // Adds record if no tag is found.
                if (documents.isEmpty) {
                    val newTag = hashMapOf(
                        "uid" to userId,
                        "tagName" to tagName
                    )
                    // Adds tag to database.
                    db.collection("tags")
                        .add(newTag)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { e ->
                        }
                }
            }
    }

    /**
     * Companion object to parse all picture data through.
     */
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

        /**
         * Creates new instance of the fragment with all these details, so picture details
         * can be displayed to the user.
         */
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