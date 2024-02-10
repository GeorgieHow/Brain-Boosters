package com.example.brainboosters.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.R
import com.example.brainboosters.model.PictureModel

class QuizPictureAdapter(
    private val context: Context,
    private val pictures: List<PictureModel>
) : RecyclerView.Adapter<QuizPictureAdapter.ViewHolder>() {

    var selectedPositions: MutableSet<Int> = mutableSetOf()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewGridItem)
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxSelection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.quiz_image_picker_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context)
            .load(pictures[position].imageUrl)
            .into(holder.imageView)

        holder.checkBox.isChecked = selectedPositions.contains(position)

        // Clear previous listener
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPositions.add(position)
                holder.imageView.alpha = 0.5f
            } else {
                selectedPositions.remove(position)
                holder.imageView.alpha = 1.0f
            }
        }

        // Optional: Toggle checkbox state when the image is clicked
        holder.itemView.setOnClickListener {
            val isCurrentlyChecked = holder.checkBox.isChecked
            holder.checkBox.isChecked = !isCurrentlyChecked
            holder.imageView.alpha = if (!isCurrentlyChecked) 0.5f else 1.0f
        }
    }

    override fun getItemCount(): Int = pictures.size
}
