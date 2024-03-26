package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.GalleryPictureAdapter
import com.example.brainboosters.model.PictureModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        db.collection("images")
            .whereEqualTo("uid", currentUserID)
            .get()
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
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.wtf("TAG", "Error getting documents: ", exception) }

    }

    override fun onItemClick(position: Int) {
        // Handle item click here
        // For example, you can navigate to a detail fragment with data from imageList[position]

        val selectedPicture = imageList[position]
        val selectedPictureID = selectedPicture.documentId

        Log.d("Firebase", "$selectedPicture, and the id? $selectedPictureID" )

        if (selectedPictureID != null) {
            db.collection("images")
                .document(selectedPictureID)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val imageUrl = documentSnapshot.getString("imageUrl")
                    val imageName = documentSnapshot.getString("name")
                    val imageYear = documentSnapshot.getLong("year").toString()
                    val imagePlace = documentSnapshot.getString("place")

                    // Create a new fragment instance with the selected picture data
                    val detailFragment = imageUrl?.let {
                        if (imageName != null) {
                            if (imageYear != null) {
                                PictureFragmentActivity.newInstance(
                                    it,
                                    imageName,
                                    imageYear,
                                    imagePlace
                                )
                            } else {
                                GalleryFragmentActivity()
                            }
                        } else {
                            // Provide a default fragment instance if imageName is null
                            GalleryFragmentActivity()
                        }
                    } ?: GalleryFragmentActivity()

                    // Replace the current fragment with the detail fragment
                    (activity as HomePageActivity).changeFragment(detailFragment)
                }
        }
    }

}