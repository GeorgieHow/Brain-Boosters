package com.example.brainboosters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.*
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A fragment which stores the second part of the photo upload process if the picture is a
 * quiz picture.
 */
class UploadFragmentPart2Activity : Fragment() {

    // Has all variables initialized here along with authentication.
    private lateinit var photoId : String
    private val selectedTags = HashSet<String>()
    private val galleryFragment = GalleryFragmentActivity()
    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.upload_part2_fragment, container, false).apply {
        photoId = arguments?.getString("photoId").toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val autoCompleteTextView = view.findViewById<AutoCompleteTextView>(R.id.tags_autocomplete_edit_text)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group)

        // Fetches tags from firebase.
        fetchTags { tags ->
            // Once tags are fetched, set up the adapter.
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tags)
            autoCompleteTextView.setAdapter(adapter)
        }

        // Add chips as user presses enter.
        autoCompleteTextView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tagName = autoCompleteTextView.text.toString()
                if (tagName.isNotEmpty()) {
                    addTagAsChip(tagName, chipGroup)
                    autoCompleteTextView.text = null
                }
                true
            } else {
                false
            }
        }

        // If upload finish button is pressed, run method to update picture.
        view.findViewById<Button>(R.id.finish_uploading_button).setOnClickListener {
            val descriptionEditText = view.findViewById<TextInputEditText>(R.id.photo_description_edit_text)
            val description = descriptionEditText.text.toString()
            mAuth.currentUser?.uid?.let { it1 ->
                updatePhotoWithTagsAndDescription(photoId,
                    it1, selectedTags.toList(), description)
            }

            // Return to gallery fragment.
            (activity as HomePageActivity).changeFragment(galleryFragment)
        }
    }

    /**
     * A method with fetches tags from firebase for the autofill.
     */
    private fun fetchTags(callback: (List<String>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserUid = mAuth.currentUser?.uid

        // Goes into the tags collection in firebase to look for tags associated with user ID.
        firestore.collection("tags")
            .whereEqualTo("uid", currentUserUid)
            .get()
            .addOnSuccessListener { snapshot ->
                //Adds tags to list.
            val tags = snapshot.documents.mapNotNull { it.getString("tagName") }.toList()
            callback(tags)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    /**
     * A method to add a tag as a chip when its entered into the text box.
     */
    private fun addTagAsChip(tagName: String, chipGroup: ChipGroup) {
        // Adds tag if not already present.
        if (selectedTags.add(tagName)) {
            val chip = Chip(context).apply {
                text = tagName
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    chipGroup.removeView(this)
                    selectedTags.remove(tagName)
                }
            }
            chipGroup.addView(chip)
        }
    }

    /**
     * A method to update the photo with tags and description.
     *
     * @param photoId the photos ID.
     * @param uid the users ID.
     * @param tags the tags associated with the picture.
     * @param description the description of the picture.
     */
    private fun updatePhotoWithTagsAndDescription(photoId: String?, uid: String, tags: List<String>, description: String) {
        val firestore = FirebaseFirestore.getInstance()
        val tagCollection = firestore.collection("tags")
        val photoCollection = firestore.collection("images")
        val priority = getSelectedPriority()

        // Updates tag collection first if it does not exist.
        tags.forEach { tagName ->
            tagCollection.whereEqualTo("tagName", tagName).get().addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    tagCollection.add(mapOf("tagName" to tagName, "uid" to uid))
                }
            }
        }

        // Update picture with all details afterwards with a hash map.
        photoId?.let {
            photoCollection.document(it).update(mapOf(
                "description" to description,
                "tags" to tags,
                "priority" to priority
            )).addOnSuccessListener {
            }.addOnFailureListener {
            }
        }
    }

    /**
     * A method to get the selected priority of the picture.
     *
     * @return returns a string for what has been selected.
     */
    private fun getSelectedPriority(): String {
        val priorityGroup = view?.findViewById<RadioGroup>(R.id.picture_priority_group)
        return when (priorityGroup?.checkedRadioButtonId) {
            //Creates a string based off which button they have selected.
            R.id.priority_low -> "Low"
            R.id.priority_normal -> "Normal"
            R.id.priority_high -> "High"
            else -> "Normal"
        }
    }

}
