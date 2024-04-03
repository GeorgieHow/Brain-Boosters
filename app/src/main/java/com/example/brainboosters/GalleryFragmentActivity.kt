package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.GalleryPictureAdapter
import com.example.brainboosters.model.PictureModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class GalleryFragmentActivity : Fragment(), GalleryPictureAdapter.OnItemClickListener {

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val imageList = mutableListOf<PictureModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.gallery_fragment, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uploadButton = view.findViewById<Button>(R.id.upload_button)
        val uploadFragment = UploadFragmentActivity()
        uploadButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(uploadFragment)
        }

        val recyclerView: RecyclerView = view.findViewById(R.id.picture_recycler_view)

        val adapter = GalleryPictureAdapter(requireContext(), imageList, this)

        val layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val currentUserID = mAuth.currentUser?.uid

        val gallerySpinner: Spinner = view.findViewById(R.id.gallery_filter_spinner)

        val spinnerAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.gallery_filters, // The array resource containing your items
            R.layout.spinner_item // Custom layout for items
        )

        gallerySpinner.adapter = spinnerAdapter

        gallerySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedFilter = parent.getItemAtPosition(position).toString()
                fetchImages(selectedFilter, adapter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

    }

    override fun onItemClick(position: Int) {
        val selectedPicture = imageList[position]
        val selectedPictureID = selectedPicture.documentId

        Log.d("Firebase", "$selectedPicture, and the id? $selectedPictureID")

        if (selectedPictureID != null) {
            db.collection("images")
                .document(selectedPictureID)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val type = documentSnapshot.getString("photoType")

                    when (type) {
                        "family album" -> {
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

                            // New part: Handling the createdAt timestamp
                            val createdAtTimestamp = documentSnapshot.getTimestamp("createdAt")
                            val createdAtDate = createdAtTimestamp?.toDate()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val createdAtString = createdAtDate?.let { dateFormat.format(it) }

                            val tagsArrayList = ArrayList(tags)

                            // Assuming your QuizFragmentActivity can handle a java.util.Date for createdAt
                            val pictureFragment = imageUrl?.let {
                                PictureFragmentActivity.newInstance(documentId, it, name, year, place, event, description, person, priority, tagsArrayList, createdAtString, type)

                            }?: GalleryFragmentActivity()

                            (activity as HomePageActivity).changeFragment(pictureFragment)
                        }
                        "quiz" -> {
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

                            // New part: Handling the createdAt timestamp
                            val createdAtTimestamp = documentSnapshot.getTimestamp("createdAt")
                            val createdAtDate = createdAtTimestamp?.toDate()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val createdAtString = createdAtDate?.let { dateFormat.format(it) }

                            val tagsArrayList = ArrayList(tags)

                            // Assuming your QuizFragmentActivity can handle a java.util.Date for createdAt
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
                        else -> {
                            (activity as HomePageActivity).changeFragment(GalleryFragmentActivity())
                        }
                    }
                }
        }
    }

    private fun fetchImages(filter: String, adapter: GalleryPictureAdapter) {
        imageList.clear() // Clear existing images

        val query = when(filter) {
            "All" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
            "Newest" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("year", Query.Direction.DESCENDING) // Newest by year
            "Oldest" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("year", Query.Direction.ASCENDING) // Oldest by year
            "Alphabetical [A-Z]" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("name", Query.Direction.ASCENDING) // Alphabetical by name
            "Alphabetical [Z-A]" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .orderBy("name", Query.Direction.DESCENDING) // Alphabetical by name
            "Quiz Photos" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .whereEqualTo("photoType", "quiz")
            "Family Album Photos" -> db.collection("images")
                .whereEqualTo("uid", mAuth.currentUser?.uid)
                .whereEqualTo("photoType", "family album")
            else -> return
        }

        query.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val photoType = document.getString("photoType")

                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id

                    val imageDescription = document.getString("description")

                    if (imageUrl != null) {
                        if(photoType == "family album"){
                            imageList.add(PictureModel(
                                imageUrl = imageUrl,
                                documentId = pictureId,
                                imageDescription = imageDescription
                            ))
                        }else{
                            val imageName = document.getString("name")
                            val imagePlace = document.getString("place")
                            val imagePerson = document.getString("person")
                            val imageYear = document.getLong("year")?.toInt()
                            val imageEvent = document.getString("event")

                            imageName?.let { PictureModel(imageUrl, it, pictureId, imagePerson,
                                imagePlace, imageEvent, imageDescription, imageYear) }
                                ?.let { imageList.add(it) }
                        }
                    }
                }
                adapter.notifyDataSetChanged() // Notify adapter
            }
            .addOnFailureListener { exception ->
                Log.wtf("TAG", "Error getting documents: ", exception)
            }
    }

}