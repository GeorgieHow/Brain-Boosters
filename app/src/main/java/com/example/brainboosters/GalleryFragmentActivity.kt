package com.example.brainboosters

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.adapter.GalleryPictureAdapter
import com.example.brainboosters.model.PictureModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GalleryFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
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

        val imageList = mutableListOf<PictureModel>()
        val recyclerView: RecyclerView = view.findViewById(R.id.picture_recycler_view)

        val adapter = GalleryPictureAdapter(requireContext(), imageList)

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
}