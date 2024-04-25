package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.widget.SearchView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.GalleryPictureAdapter
import com.example.brainboosters.model.PictureModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The gallery fragment which displays all the pictures the user has uploaded.
 * Lets user search and filyer through them.
 */
class GalleryFragmentActivity : Fragment(), GalleryPictureAdapter.OnItemClickListener {

    // Firebase instances for authentication and database.
    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Image list for all pictures.
    private val imageList = mutableListOf<PictureModel>()

    //  Variables needed for adapter and search view.
    private lateinit var adapter: GalleryPictureAdapter
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.gallery_fragment, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Defines upload button, so user can navigate to the upload page from here.
        val uploadButton = view.findViewById<Button>(R.id.upload_button)
        val uploadFragment = UploadFragmentActivity()
        uploadButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(uploadFragment)
        }

        // Initializes and creates the layout for the recycler view in this fragment.
        recyclerView = view.findViewById(R.id.picture_recycler_view)
        adapter = GalleryPictureAdapter(requireContext(), imageList, this)
        val layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Spinner set up so users can filter through images.
        val gallerySpinner: Spinner = view.findViewById(R.id.gallery_filter_spinner)
        val spinnerAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.gallery_filters,
            R.layout.spinner_item
        )
        gallerySpinner.adapter = spinnerAdapter

        // A listener to handle when a user clicks on it and selects a filter.
        gallerySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                // Re-fetches images with the filter in place.
                val selectedFilter = parent.getItemAtPosition(position).toString()
                fetchImages(selectedFilter, adapter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        // Automatically fetch all images at the start.
        fetchImages("All", adapter)

        // Set up search view, so user can search pictures by their tag.
        searchView = view.findViewById(R.id.search_view)
        searchView.isEnabled = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    fetchImages("All", adapter)
                }
                // Filter the pictures based on what the user has entered.
                else {
                    adapter.filter(newText)
                }
                return true
            }
        })


    }

    /**
     * Handler for when a user clicks on a picture - takes them to the details of said picture.
     */
    override fun onItemClick(position: Int) {
        val selectedPicture = adapter.getItemAt(position)
        val selectedPictureID = selectedPicture.documentId

        // If the pictures ID isn't null it will fetch details
        if (selectedPictureID != null) {
            db.collection("images")
                .document(selectedPictureID)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val type = documentSnapshot.getString("photoType")

                    // Makes sure it parses through the right details as there are
                    // two picture types.
                    when (type) {
                        "family album" -> {
                            // Extracts family album picture details.
                            val documentId = documentSnapshot.id
                            val imageUrl = documentSnapshot.getString("imageUrl")
                            val name = documentSnapshot.getString("name")?: "Name"
                            val event = documentSnapshot.getString("event")?: "Event"
                            val description = documentSnapshot.getString("description")
                            val person = documentSnapshot.getString("person")?: "Person"
                            val place = documentSnapshot.getString("place")?: "Place"
                            val priority = documentSnapshot.getString("priority")?: "Normal"
                            val tags = documentSnapshot.get("tags") as List<String>? ?: listOf()
                            val year = documentSnapshot.getLong("year")?.toString() ?: "Unknown"
                            val createdAtTimestamp = documentSnapshot.getTimestamp("createdAt")
                            val createdAtDate = createdAtTimestamp?.toDate()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val createdAtString = createdAtDate?.let { dateFormat.format(it) }

                            val tagsArrayList = ArrayList(tags)

                            //Loads with all these details.
                            val pictureFragment = imageUrl?.let {
                                PictureFragmentActivity.newInstance(documentId, it, name, year, place, event, description, person, priority, tagsArrayList, createdAtString, type)

                            }?: GalleryFragmentActivity()

                            (activity as HomePageActivity).changeFragment(pictureFragment)
                        }
                        "quiz" -> {
                            // Extracts quiz picture details.
                            val documentId = documentSnapshot.id
                            val imageUrl = documentSnapshot.getString("imageUrl")
                            val name = documentSnapshot.getString("name")
                            val event = documentSnapshot.getString("event")
                            val description = documentSnapshot.getString("description")
                            val person = documentSnapshot.getString("person")
                            val place = documentSnapshot.getString("place")
                            val priority = documentSnapshot.getString("priority")?: "Normal"
                            val tags = documentSnapshot.get("tags") as List<String>? ?: listOf()
                            val year = documentSnapshot.getLong("year")?.toString() ?: "Unknown"
                            val createdAtTimestamp = documentSnapshot.getTimestamp("createdAt")
                            val createdAtDate = createdAtTimestamp?.toDate()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val createdAtString = createdAtDate?.let { dateFormat.format(it) }

                            val tagsArrayList = ArrayList(tags)

                            //Loads with all these details.
                            val pictureFragment = imageUrl?.let {
                                if (name != null) {
                                    if (event != null) {
                                        PictureFragmentActivity.newInstance(
                                            documentId, it, name, year, place, event, description, person, priority, tagsArrayList, createdAtString, type
                                        )
                                    }
                                    else{
                                        GalleryFragmentActivity()
                                    }
                                }
                                else{
                                    GalleryFragmentActivity()
                                }
                            }?: GalleryFragmentActivity()

                            (activity as HomePageActivity).changeFragment(pictureFragment)

                        }
                        // Navigates back to Gallery Fragment if nothing exists or runs into error.
                        else -> {
                            (activity as HomePageActivity).changeFragment(GalleryFragmentActivity())
                        }
                    }
                }
        }
    }

    /**
     * Fetches images from Firestore based on the filter selected in the spinner
     * and updates the gallery adapter with the fetched images.
     *
     * @param filter The filter the user has selected.
     * @param adapter The adapter which is linked to the recycler view.
     */
    private fun fetchImages(filter: String, adapter: GalleryPictureAdapter) {

        // Clears the list so no pictures remain.
        imageList.clear()

        // Creates queries for when the user selects something on the spinner.
        val query = when(filter) {
            "All" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
            "Newest" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("year", Query.Direction.DESCENDING)
            "Oldest" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("year", Query.Direction.ASCENDING)
            "Alphabetical [A-Z]" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("name", Query.Direction.ASCENDING)
            "Alphabetical [Z-A]" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("name", Query.Direction.DESCENDING)
            "Quiz Photos" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .whereEqualTo("photoType", "quiz")
            "Family Album Photos" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .whereEqualTo("photoType", "family album")
            else -> return
        }

        // Executes the query based on the filter selected.
        query.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val photoType = document.getString("photoType")
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageDescription = document.getString("description")
                    val tags = document.get("tags") as? List<String> ?: emptyList()

                    // Adds image to the image list, if it isn't null.
                    if (imageUrl != null) {
                        if(photoType == "family album"){
                            imageList.add(PictureModel(
                                imageUrl = imageUrl,
                                documentId = pictureId,
                                imageDescription = imageDescription,
                                tags = tags
                            ))
                        }else{
                            // Adds more details if the photo is a quiz photo instead.
                            val imageName = document.getString("name")
                            val imagePlace = document.getString("place")
                            val imagePerson = document.getString("person")
                            val imageYear = document.getLong("year")?.toInt()
                            val imageEvent = document.getString("event")

                            // Adds image onto image list.
                            imageName?.let { PictureModel(imageUrl, it, pictureId, imagePerson,
                                imagePlace, imageEvent, imageDescription, imageYear, null, tags) }
                                ?.let { imageList.add(it) }
                        }
                    }
                }

                // Lets recycler know there has been an update of items within the recylcer view.
                adapter.setList(imageList)
                adapter.notifyDataSetChanged()
                searchView.isEnabled = true
            }
            .addOnFailureListener { exception ->
                // Shows snack bar to show it failed to get photos.
                Snackbar.make(recyclerView, "Failed to fetch images. Check your connection.",
                    Snackbar.LENGTH_LONG).show()
            }
    }

}