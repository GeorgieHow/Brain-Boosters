package com.example.brainboosters.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.R
import com.example.brainboosters.model.PictureModel
import com.squareup.picasso.Picasso
import java.util.Locale


class GalleryPictureAdapter(private val context: Context,
                            private var pictureList: MutableList<PictureModel>,
                            private val itemClickListener: OnItemClickListener) :
                            RecyclerView.Adapter<GalleryPictureAdapter.ViewHolder>(){

    private var imageListFiltered: MutableList<PictureModel> = mutableListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.picture_image_view)
    }

    fun setList(newList: MutableList<PictureModel>) {
        Log.d("GalleryAdapter", "$newList")
        pictureList.clear()
        pictureList = newList.toMutableList()
        imageListFiltered.clear()
        imageListFiltered = newList.toMutableList()
        Log.d("GalleryAdapter", "List set in adapter. New size: ${imageListFiltered.size}")

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_card_view, parent,
            false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageListFiltered.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageModel = imageListFiltered[position]
        // Use Glide to load the image
        Glide.with(context)
            .load(imageModel.imageUrl)
            .into(holder.imageView)
        //holder.pictureNameTextView.text = imageModel.imageName

        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(position)
        }
    }

    fun filter(query: String) {
        Log.d("Query", "Filtering for: $query")
        val lowercaseQuery = query.lowercase(Locale.getDefault())

        // Clear the filtered list before adding new filtered data
        imageListFiltered.clear()

        if (query.isEmpty()) {
            // If the query is empty, add all pictures from the original list to the filtered list
            imageListFiltered.addAll(pictureList)
        } else {
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

    fun getItemAt(position: Int): PictureModel {
        return imageListFiltered[position]
    }

}