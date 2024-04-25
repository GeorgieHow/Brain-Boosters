package com.example.brainboosters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.FamilyAlbumAdapter
import com.example.brainboosters.model.PictureModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

/**
 * A class which handles all code relating to the Family Album Fragment.
 */
class FamilyAlbumActivity : Fragment(){

    //Initialises variables needed for this class.
    private var db = FirebaseFirestore.getInstance()
    private var mAuth = FirebaseAuth.getInstance()
    private lateinit var recyclerView: RecyclerView

    private var adapter = FamilyAlbumAdapter(mutableListOf())
    val imageList = mutableListOf<PictureModel>()
    val positionToImageListIndexMap = hashMapOf<Int, Int>()

    /**
     * Inflates the right view when created so it displays the family album XML layout when
     * navigated to.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.family_album_fragment, container, false).apply {

    }

    /**
     * Once created, this runs to populate the view itself.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Finds recycler view and establishes how many columns there will be in it.
        recyclerView = view.findViewById(R.id.family_album_recycler_view)
        val numberOfColumns = 4

        // Uses layout manager to create a grid layout for the recycler view.
        recyclerView.layoutManager = GridLayoutManager(requireContext(), numberOfColumns). apply{
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
                override fun getSpanSize(position: Int): Int {
                    // If the item at that position is a header, make it so it is as long as
                    // all columns combined to make the header.
                    return if (adapter.isHeader(position)) {
                        numberOfColumns
                    }
                    // Otherwise, it should only be worth one - gives just the pictures a grid
                    // effect.
                    else{
                        1
                    }
                }
            }

        }

        // Sets the recycler views adapter to the already established adapter, then fetches images.
        recyclerView.adapter = adapter
        fetchImages()


        // Sets up back button, so users can naviagte back to the main page from this fragment.
        val backButton = view.findViewById<Button>(R.id.back_button)
        val homePage = HomeFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(homePage)
        }

        // On clicking on a picture, takes the user to a view of those picture details.
        adapter.setOnItemClickListener(object: FamilyAlbumAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {

                // Looks through the index map, and gets the selected picture from there.
                positionToImageListIndexMap[position]?.let { imageListIndex ->
                    val selectedPicture = imageList[imageListIndex]
                    val selectedPictureID = selectedPicture.documentId

                    // If the pictures id isn't null, fetch the details of that picture from
                    // Firebase Database.
                    if (selectedPictureID != null) {
                        db.collection("images")
                            .document(selectedPictureID)
                            .get()
                            .addOnSuccessListener {
                                    // Gets document details and assigns them to variables.
                                    document ->
                                val imageUrl = document.getString("imageUrl")
                                val pictureId = document.id
                                val imageDescription =
                                    document.getString("description")
                                val timestamp = document.getTimestamp("createdAt")

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

    /**
     * Fetches images from Firestore where the 'uid' matches the current user and
     * 'photoType' is 'family album'. Images are ordered by creation date in descending order.
     */
    private fun fetchImages() {

        // Retrieves user's ID
        val currentUserID = mAuth.currentUser?.uid ?: return

        // Queries the database to find images that match the uid and photo type.
        db.collection("images")
            .whereEqualTo("uid", currentUserID)
            .whereEqualTo("photoType", "family album")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {

                    // Gets details out of document.
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageDescription = document.getString("description")
                    val timestamp = document.getTimestamp("createdAt")

                    // Creates PictureModel with details gotten from document.
                    val picture = PictureModel(
                        imageUrl = imageUrl,
                        documentId = pictureId,
                        imageDescription = imageDescription,
                        timestamp = timestamp
                    )

                    // Adds to list of images.
                    imageList.add(picture)
                }

                // Creates a grouped list of the pictures using the group method, updates recylcer
                // to display them.
                val groupedPictures = groupPicturesByMonth(imageList)
                adapter.updateData(groupedPictures)
                updatePositionMapping(groupedPictures)
            }
            .addOnFailureListener {
                // Shows snack bar to show it failed to get photos.
                Snackbar.make(recyclerView, "Failed to fetch images. Check your connection.",
                    Snackbar.LENGTH_LONG).show()
            }
    }

    /**
     * Updates mapping from list positions to indices in the original image list to
     * stop the wrong picture being gotten when clicked.
     *
     * @param groupedItems List of grouped items where pictures are indexed.
     */
    private fun updatePositionMapping(groupedItems: List<Any>) {
        // Clears prior mappings to make sure nothings there when first mapping.
        positionToImageListIndexMap.clear()
        var photoIndex = 0

        // For every item, if its a picture, it will map the picture om the list to the picture
        // within the recycler view.
        groupedItems.forEachIndexed { index, item ->
            if (item is PictureModel) {
                positionToImageListIndexMap[index] = photoIndex++
            }
        }
    }

    /**
     * Groups pictures by the month and year of their timestamp, so they can be
     * ordered within the recycler view.
     *
     * @param pictures List of PictureModel objects.
     * @return A list containing grouped items, with both headers and pictures.
     */
    private fun groupPicturesByMonth(pictures: List<PictureModel>): List<Any> {

        // Creates a date formatter so the year nad month can be found.
        val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())

        // Groups all the pictures by that formatter, so they are stored within the same month
        // and year.
        val grouped = pictures.groupBy {
            formatter.format(it.timestamp?.toDate() ?: Date())
        }

        // Make each group so they each have a month along with all photos posted in that month.
        val groupedItems = mutableListOf<Any>()
        grouped.forEach { (month, photos) ->
            groupedItems.add(month)
            groupedItems.addAll(photos)
        }
        return groupedItems
    }
}