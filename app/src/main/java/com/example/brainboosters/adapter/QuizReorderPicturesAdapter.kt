package com.example.brainboosters.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brainboosters.R

/**
 * Adapter for a RecyclerView that displays the pictures for the reorder section of the quiz.
 * Allows users to drag the pictures around to move them in the correct order.
 *
 * @param urls Urls of the pictures that have been present in the quiz.
 * @param context The context where the adapter is being used.
 */
class QuizReorderPicturesAdapter(
    private var urls: MutableList<String>,
    private val context: Context
) : RecyclerView.Adapter<QuizReorderPicturesAdapter.ViewHolder>() {

    /**
     * This ViewHolder is what the data is actually displayed in. As only the picture needs to be
     * shown, theres only an ImageView.
     *
     * @param itemView The view of where the ImageView is located.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    /**
     * Inflates the layout for items in the RecyclerView.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The type of the new View.
     * @return A new ViewHolder that holds the View.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_reorder_layout, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds each item in the ViewHolder to the data in the url list, before loading the
     * image into the ImageView via 'Glide'.
     *
     * @param holder The ViewHolder which should be updated.
     * @param position The position of the item within the data set.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = urls[position]
        Glide.with(context).load(imageUrl).into(holder.imageView)
    }

    /**
     * Gets count of items within the urls list.
     */
    override fun getItemCount(): Int = urls.size

    /**
     * Getter for the urls list, so it can be accessed in other files.
     *
     * @return A mutable list of all the urls.
     */
    fun getItems(): MutableList<String>{
        return urls
    }

    /**
     * A method that clears the urls and updates them with all with the new urls, notifying the
     * adapter that theres been a change in the data.
     *
     * @param newUrls
     */
    fun updateItems(newUrls: List<String>) {
        urls.clear()
        urls.addAll(newUrls)
        notifyDataSetChanged()
    }
}
