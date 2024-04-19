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

/**
 * An adapter used for the pictures used in a quiz. Will display on the results page#after a quiz
 * has happened.
 *
 * @param context The environment where the adapter is used.
 * @param imagesList List of images to be displayed.
 */
class ImagesAdapter(private val context: Context, private val imagesList: List<PictureModel>) : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

    /**
     * ViewHolder for holding each image item's view within the RecyclerView.
     *
     * @param view The individual item view.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    /**
     * Creates a ViewHolder for each item in the RecyclerView. This method inflates the layout for individual
     * items.
     *
     * @param parent The container for the new view.
     * @param viewType Type of view to create.
     * @return A ViewHolder that contains the view.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.picture_item_results, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds an image to a ViewHolder at the specified position.
     *
     * @param holder The ViewHolder to update.
     * @param position The index of the image in the list.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val picture = imagesList[position]

        // Uses glide to put picture in the image view.
        picture.imageUrl?.let {
            Glide.with(context)
                .load(it)
                .into(holder.imageView)
        }
    }

    /**
     * Gets item count of the amount of items within the recycler view.
     * */
    override fun getItemCount() = imagesList.size
}