package com.example.brainboosters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.*
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class UploadFragmentPart2Activity : Fragment() {

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

        val autoCompleteTextView = view.findViewById<MaterialAutoCompleteTextView>(R.id.tags_autocomplete_text_view)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group) // Assuming you have a ChipGroup in your layout

        // Fetch tags from Firebase
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



        view.findViewById<Button>(R.id.finish_uploading_button).setOnClickListener {
            val descriptionEditText = view.findViewById<TextInputEditText>(R.id.photo_description_edit_text)
            val description = descriptionEditText.text.toString() // Get this from your input field
            mAuth.currentUser?.uid?.let { it1 ->
                updatePhotoWithTagsAndDescription(photoId,
                    it1, selectedTags.toList(), description)
            }

            (activity as HomePageActivity).changeFragment(galleryFragment)
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

    private fun updatePhotoWithTagsAndDescription(photoId: String?, uid: String, tags: List<String>, description: String) {
        val firestore = FirebaseFirestore.getInstance()
        val tagCollection = firestore.collection("tags")
        val photoCollection = firestore.collection("images")
        val priority = getSelectedPriority()

        // Update tags collection
        tags.forEach { tagName ->
            tagCollection.whereEqualTo("tagName", tagName).get().addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    tagCollection.add(mapOf("tagName" to tagName, "uid" to uid))
                }
                // If the tag exists, we do nothing
            }
        }

        // Once tags are ensured to be in Firestore, update the photo document
        // Consider moving this inside the forEach if you need to wait for all tag checks/additions to complete
        photoId?.let {
            photoCollection.document(it).update(mapOf(
                "description" to description,
                "tags" to tags,
                "priority" to priority
            )).addOnSuccessListener {
                // Handle success - perhaps navigate back or show a message
            }.addOnFailureListener {
                // Handle failure
            }
        }
    }

    private fun getSelectedPriority(): String {
        val priorityGroup = view?.findViewById<RadioGroup>(R.id.picture_priority_group)
        return when (priorityGroup?.checkedRadioButtonId) {
            R.id.priority_low -> "Low"
            R.id.priority_normal -> "Normal"
            R.id.priority_high -> "High"
            else -> "Normal"
        }
    }

}
