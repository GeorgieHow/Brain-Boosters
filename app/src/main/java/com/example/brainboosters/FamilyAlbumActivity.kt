package com.example.brainboosters

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.FamilyAlbumAdapter
import com.example.brainboosters.model.PictureModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date
import java.util.Timer
import kotlin.concurrent.schedule


class FamilyAlbumActivity : Fragment(){

    private var db = FirebaseFirestore.getInstance()
    private var mAuth = FirebaseAuth.getInstance()
    private lateinit var recyclerView: RecyclerView

    private var adapter = FamilyAlbumAdapter(mutableListOf())
    val imageList = mutableListOf<PictureModel>()
    val positionToImageListIndexMap = hashMapOf<Int, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.family_album_fragment, container, false).apply {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.family_album_recycler_view)

        val numberOfColumns = 4

        recyclerView.layoutManager = GridLayoutManager(requireContext(), numberOfColumns). apply{
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
                override fun getSpanSize(position: Int): Int {
                    return if (adapter.isHeader(position)) {
                        numberOfColumns
                    } else{
                        1
                    }
                }
            }

        }

        recyclerView.adapter = adapter
        fetchImages()


        val backButton = view.findViewById<Button>(R.id.back_button)
        val homePage = HomeFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(homePage)
        }

        adapter.setOnItemClickListener(object: FamilyAlbumAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                positionToImageListIndexMap[position]?.let { imageListIndex ->
                    val selectedPicture = imageList[imageListIndex]
                    val selectedPictureID = selectedPicture.documentId

                    if (selectedPictureID != null) {
                        db.collection("images")
                            .document(selectedPictureID)
                            .get()
                            .addOnSuccessListener { documentSnapshot ->
                                val imageUrl = documentSnapshot.getString("imageUrl")
                                val pictureId = documentSnapshot.id
                                val imageDescription =
                                    documentSnapshot.getString("description")
                                val timestamp = documentSnapshot.getTimestamp("createdAt")

                                Log.wtf("HELP", "Clickable: $position Picture: $imageUrl")

                                // Create a new fragment instance with the selected picture data
                                val familyAlbumPictureFragment = imageUrl?.let {
                                    if (pictureId != null) {
                                        if (imageDescription != null) {
                                            FamilyAlbumPictureActivity.newInstance(
                                                imageUrl,
                                                pictureId,
                                                imageDescription,
                                                timestamp.toString()
                                            )
                                        } else {
                                            FamilyAlbumActivity()
                                        }
                                    } else {
                                        // Provide a default fragment instance if imageName is null
                                        FamilyAlbumActivity()
                                    }
                                } ?: FamilyAlbumActivity()


                                // Replace the current fragment with the detail fragment
                                (activity as HomePageActivity).changeFragment(
                                    familyAlbumPictureFragment
                                )
                            }
                    }
                }
            }
        })
    }


    private fun fetchImages() {
        val currentUserID = mAuth.currentUser?.uid ?: return

        db.collection("images")
            .whereEqualTo("uid", currentUserID)
            .whereEqualTo("photoType", "family album")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageDescription = document.getString("description")
                    val timestamp = document.getTimestamp("createdAt")

                    Log.wtf("Timestamp", "$timestamp")

                    val picture = PictureModel(
                        imageUrl = imageUrl,
                        documentId = pictureId,
                        imageDescription = imageDescription,
                        timestamp = timestamp
                    )

                    imageList.add(picture)
                }
                val groupedPictures = groupPicturesByMonth(imageList)
                adapter.updateData(groupedPictures)
                updatePositionMapping(groupedPictures)
                Log.d("FetchImages", "Final grouped items count: ${groupedPictures.size}")

            }
            .addOnFailureListener { exception ->
                // Handle any errors here
            }


    }
    private fun updatePositionMapping(groupedItems: List<Any>) {
        positionToImageListIndexMap.clear()
        var photoIndex = 0 // Index for imageList
        groupedItems.forEachIndexed { index, item ->
            if (item is PictureModel) {
                positionToImageListIndexMap[index] = photoIndex++
            }
            // Headers are ignored in the mapping
        }
    }

    private fun groupPicturesByMonth(pictures: List<PictureModel>): List<Any> {
        val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())

        val grouped = pictures.groupBy {
            formatter.format(it.timestamp?.toDate() ?: Date())
        }

        val groupedItems = mutableListOf<Any>()
        grouped.forEach { (month, photos) ->
            groupedItems.add(month)
            groupedItems.addAll(photos)
        }
        Log.d("Grouping", "Grouped Items: ${grouped.keys.joinToString()}")
        return groupedItems
    }
}