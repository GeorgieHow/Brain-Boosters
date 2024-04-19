package com.example.brainboosters.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.R

class QuizReorderPicturesAdapter(
    private var items: MutableList<String>,  // URLs of images
    private val context: Context
) : RecyclerView.Adapter<QuizReorderPicturesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_reorder_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = items[position]
        Glide.with(context).load(imageUrl).into(holder.imageView)
    }

    override fun getItemCount(): Int = items.size

    fun getItems(): MutableList<String>{
        return items
    }

    fun updateItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
