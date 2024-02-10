package com.example.brainboosters

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.QuizPictureAdapter
import com.example.brainboosters.model.PictureModel
import com.google.firebase.firestore.FirebaseFirestore

class QuizImageSelectionActivity : Fragment(){

    private lateinit var recyclerViewImages: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.quiz_image_selection, container, false)
        recyclerViewImages = view.findViewById(R.id.image_selector_recycler_view)


        recyclerViewImages.layoutManager = GridLayoutManager(requireContext(), 3)

        // Fetch pictures and set up the adapter within the completion block
        fetchPicturesFromFirestore { pictures ->
            activity?.runOnUiThread {
                recyclerViewImages.adapter = QuizPictureAdapter(requireContext(), pictures)
            }
        }

        return view
    }

    fun fetchPicturesFromFirestore(completion: (List<PictureModel>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val picturesList = mutableListOf<PictureModel>()

        db.collection("images").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageName = document.getString("name")

                    val picture = imageName?.let {
                        if (imageUrl != null) {
                            val picture = PictureModel(imageUrl, it, pictureId)
                            picturesList.add(picture)
                        }
                    }
                }
                completion(picturesList)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

}