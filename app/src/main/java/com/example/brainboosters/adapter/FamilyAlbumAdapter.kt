package com.example.brainboosters.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.R
import com.example.brainboosters.model.PictureModel

class FamilyAlbumAdapter(private val items: MutableList<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> VIEW_TYPE_HEADER
            is PictureModel -> VIEW_TYPE_PHOTO
            else -> throw IllegalArgumentException("Invalid type of data $position")
        }
    }

    fun numberOfHeadersBefore(position: Int): Int {
        var headerCount = 0
        for (i in 0 until position) {
            if (isHeader(i)) {
                headerCount++
            }
        }
        return headerCount
    }

    fun isHeader(position: Int): Boolean {
        return items[position] is String // Assuming headers are of type String
    }
    var data: List<PictureModel> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()  // Notify the adapter when the data changes so the UI can refresh
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_PHOTO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
                PhotoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    fun updateData(newData: List<Any>) {
        items.clear()
        items.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_HEADER -> (holder as HeaderViewHolder).bind(items[position] as String)
            VIEW_TYPE_PHOTO -> (holder as PhotoViewHolder).bind(items[position] as PictureModel, listener)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.header_title)

        fun bind(header: String) {
            title.text = header
            itemView.setOnClickListener {
            }
        }
    }
    fun countHeadersUpTo(position: Int): Int {
        var headerCount = 0
        for (i in 0 until position) {
            if (isHeader(i)) headerCount++
        }
        return headerCount
    }

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.imageView_photo)

        fun bind(pictureModel: PictureModel, listener: OnItemClickListener?) {
            Glide.with(imageView.context) // Provide a context
                .load(pictureModel.imageUrl) // Load the image URL
                .placeholder(R.drawable.shadow_drawable)
                .into(imageView) // The target ImageView

            itemView.setOnClickListener{
                listener?.onItemClick(absoluteAdapterPosition)
            }
        }
    }

    private var listener: OnItemClickListener? = null
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
    override fun getItemCount() = items.size
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PHOTO = 1
    }
}

