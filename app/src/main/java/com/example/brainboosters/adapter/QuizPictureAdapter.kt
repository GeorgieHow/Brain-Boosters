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

/**
 * Adapter for a RecyclerView that displays a list of images for selection in a quiz.
 * Allows users to select or deselect images via checkboxes.
 *
 * @param context The context where the adapter is being used, needed for inflating layouts.
 * @param pictures List of PictureModel containing data for each image.
 */
class QuizPictureAdapter(
    private val context: Context,
    private val pictures: List<PictureModel>
) : RecyclerView.Adapter<QuizPictureAdapter.ViewHolder>() {

    // Tracks the positions of selected images in the RecyclerView.
    private var selectedPositions: MutableSet<Int> = mutableSetOf()

    /**
     * Provides a reference to the views for each data item. Holds the ImageView and CheckBox.
     *
     * @param view The individual item view.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewGridItem)
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxSelection)
    }

    /**
     * Inflates the layout from XML when a new ViewHolder is required.
     *
     * @param parent The container for the new view.
     * @param viewType Type of view to create, not used here as all items are the same.
     * @return A new ViewHolder for displaying images.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.quiz_image_picker_view, parent,
            false)
        return ViewHolder(view)
    }

    /**
     * Binds each image to a ViewHolder at a specified position. Sets up the checkbox status and
     * click listeners.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // Use Glide to load image.
        Glide.with(context)
            .load(pictures[position].imageUrl)
            .into(holder.imageView)

        // Set checkbox status based on whether the position is in the selected set.
        holder.checkBox.isChecked = selectedPositions.contains(position)

        // Clear previous listener
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Add position to selected set if checked.
            if (isChecked) {
                selectedPositions.add(position)

                // Makes it transparent.
                holder.imageView.alpha = 0.5f
            }
            // Remove position from selected set if unchecked.
            else {
                selectedPositions.remove(position)

                // Reverses transparency.
                holder.imageView.alpha = 1.0f
            }
        }
        // Handle image view click to toggle checkbox state.
        holder.itemView.setOnClickListener {
            val isCurrentlyChecked = holder.checkBox.isChecked
            holder.checkBox.isChecked = !isCurrentlyChecked
            holder.imageView.alpha = if (!isCurrentlyChecked) 0.5f else 1.0f
        }
    }


    /**
     * Returns the total number of items in the data set held by the adapter.
     */
    override fun getItemCount(): Int = pictures.size

    /**
     * Retrieves a list of PictureModel objects representing the selected images.
     *
     * @return List of selected PictureModel based on the positions stored in selectedPositions.
     */
    fun getSelectedPictures(): List<PictureModel>{
        return selectedPositions.map {
            position -> pictures[position]
        }
    }
}
