package com.example.brainboosters

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.model.PictureModel


class ImagesAdapter(private val context: Context, private val imagesList: List<PictureModel>) : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.picture_item_results, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val picture = imagesList[position]
        picture.imageUrl?.let {
            Glide.with(context)
                .load(it)
                .into(holder.imageView)
        }
    }

    override fun getItemCount() = imagesList.size
}