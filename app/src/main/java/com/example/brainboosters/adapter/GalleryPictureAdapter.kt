package com.example.brainboosters.adapter

import android.content.Context
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

class GalleryPictureAdapter(private val context: Context,
                            private val pictureList: List<PictureModel>,
                            private val itemClickListener: OnItemClickListener) :
                            RecyclerView.Adapter<GalleryPictureAdapter.ViewHolder>(){

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.picture_image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_card_view, parent,
            false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return pictureList.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageModel = pictureList[position]
        // Use Glide to load the image
        Glide.with(context)
            .load(imageModel.imageUrl)
            .into(holder.imageView)
        //holder.pictureNameTextView.text = imageModel.imageName

        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(position)
        }
    }


}