package com.example.brainboosters.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.R
import com.example.brainboosters.model.PictureModel
import java.util.Locale

/**
 * Adapter for a RecyclerView displaying a gallery of pictures. It supports filtering and item
 * clicks.
 *
 * @param context The context where the adapter is being used.
 * @param pictureList A mutable list of PictureModel, the initial list of images.
 * @param itemClickListener Listener for handling clicks on items.
 */
class GalleryPictureAdapter(private val context: Context,
                            private var pictureList: MutableList<PictureModel>,
                            private val itemClickListener: OnItemClickListener) :
                            RecyclerView.Adapter<GalleryPictureAdapter.ViewHolder>(){

    // Filtered list of images that is displayed in the RecyclerView.
    private var imageListFiltered: MutableList<PictureModel> = mutableListOf()

    /**
     * Provides a reference to the views for each data item.
     *
     * @param itemView The view corresponding to each data item.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.picture_image_view)
    }

    /**
     * Updates the main and filtered list with new data and logs the new size.
     *
     * @param newList The new list of PictureModel to replace the old list.
     */
    fun setList(newList: MutableList<PictureModel>) {
        Log.d("GalleryAdapter", "$newList")
        // Resets whole picture list.
        pictureList.clear()
        pictureList = newList.toMutableList()

        // Resets filtered picture list.
        imageListFiltered.clear()
        imageListFiltered = newList.toMutableList()
        Log.d("GalleryAdapter", "List set in adapter. New size: ${imageListFiltered.size}")
    }

    /**
     * Inflates the layout for items in the RecyclerView.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The type of the new View.
     * @return A new ViewHolder that holds the View.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_card_view, parent,
            false)
        return ViewHolder(view)
    }

    /**
     * Returns the total number of items in the filtered list.
     *
     * @return Size of the filtered list.
     */
    override fun getItemCount(): Int {
        return imageListFiltered.size
    }

    /**
     * Interface for handling item click events.
     */
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    /**
     * Binds each item in the ViewHolder to its data element from the filtered list.
     *
     * @param holder The ViewHolder which should be updated.
     * @param position The position of the item within the data set.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageModel = imageListFiltered[position]

        // Use Glide to load the image.
        Glide.with(context)
            .load(imageModel.imageUrl)
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(position)
        }
    }

    /**
     * Filters the images based on a query string. Updates the filtered list and logs the results.
     *
     * @param query The text to filter the images by.
     */
    fun filter(query: String) {
        Log.d("Query", "Filtering for: $query")
        val lowercaseQuery = query.lowercase(Locale.getDefault())

        // Clear the filtered list before adding new filtered data.
        imageListFiltered.clear()

        // If the query is empty, add all pictures from the original list to the filtered list.
        if (query.isEmpty()) {
            imageListFiltered.addAll(pictureList)
        }
        // Otherwise, use the text entered in the search.
        else {
            // Filter the original list and add the results to the filtered list
            val filteredResults = pictureList.filter { pictureModel ->
                pictureModel.tags.any { tag ->
                    tag.lowercase(Locale.getDefault()).contains(lowercaseQuery)
                }
            }
            Log.d("Filter", "Filtered list size: ${filteredResults.size}")
            imageListFiltered.addAll(filteredResults)
        }

        // Notify the RecyclerView adapter that the data has changed so it can update
        notifyDataSetChanged()
    }

    /**
     * Retrieves the PictureModel at the specified position in the filtered list.
     *
     * @param position The index of the item to retrieve.
     * @return The PictureModel at the specified index.
     */
    fun getItemAt(position: Int): PictureModel {
        return imageListFiltered[position]
    }

}