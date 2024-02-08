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
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageName = document.getString("name")

                    if (imageUrl != null) {
                        imageName?.let { PictureModel(imageUrl, it, pictureId) }
                            ?.let { imageList.add(it) }
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.d("TAG", "Error getting documents: ", exception)
            }

    }

    override fun onItemClick(position: Int) {
        // Handle item click here
        // For example, you can navigate to a detail fragment with data from imageList[position]

        val selectedPicture = imageList[position]
        val selectedPictureID = selectedPicture.documentId

        Log.d("Firebase", "$selectedPicture, and the id? $selectedPictureID" )

        db.collection("images")
            .document(selectedPictureID)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val imageUrl = documentSnapshot.getString("imageUrl")
                val imageName = documentSnapshot.getString("name")

                // Create a new fragment instance with the selected picture data
                val detailFragment = imageUrl?.let {
                    if (imageName != null) {
                        PictureFragmentActivity.newInstance(
                            it,
                            imageName
                        )
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