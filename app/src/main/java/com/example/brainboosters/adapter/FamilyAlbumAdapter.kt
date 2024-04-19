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

/**
 * Adapter for a RecyclerView that manages the family album in the FamilyAlbumActivity.
 *
 * @property items A mutable list of items where each item can be either a header (String)
 * or a photo (PictureModel).
 */
class FamilyAlbumAdapter(private val items: MutableList<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listener: OnItemClickListener? = null

    /**
     * Returns the view type of the item at the specified position.
     *
     * @param position Index of the item.
     * @return An integer representing the view type.
     */
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> VIEW_TYPE_HEADER
            is PictureModel -> VIEW_TYPE_PHOTO
            else -> throw IllegalArgumentException("Invalid type of data $position")
        }
    }

    /**
     * A method to tell what positions within the recyclerview are headers. Used to
     * ignore headers when clicking on pictures.
     *
     * @param position The current position to check against.
     * @return Count of headers before the specified position.
     */
    fun isHeader(position: Int): Boolean {
        return items[position] is String
    }

    /**
     * Updates the list of PictureModels managed by this adapter.
     * This setter also notifies the adapter to refresh the view.
     */
    var data: List<PictureModel> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**
     * Creates new ViewHolder instances for different view types (headers or photos).
     *
     * @param parent The container for the new view.
     * @param viewType The type of view to create.
     * @return A ViewHolder for the new view.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            // Inflates the item_header layout if a header.
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent,
                    false)
                HeaderViewHolder(view)
            }
            // Inflates the item_photo layout if a photo.
            VIEW_TYPE_PHOTO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent,
                    false)
                PhotoViewHolder(view)
            }
            // Otherwise, shouldn't be there.
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    /**
     * Used to update the adapter's data and refreshes the RecyclerView.
     *
     * @param newData The data that replaces the old data.
     */
    fun updateData(newData: List<Any>) {
        // Makes sure to clear before adding so list has nothing unnecessary.
        items.clear()
        items.addAll(newData)
        notifyDataSetChanged()
    }

    /**
     * Binds the data to the ViewHolder.
     *
     * @param holder holder The ViewHolder to be updated.
     * @param position position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_HEADER -> (holder as HeaderViewHolder).bind(items[position] as String)

            // Adds listener to photo, so it is clickable and can navigate to photo details.
            VIEW_TYPE_PHOTO -> (holder as PhotoViewHolder).bind(items[position] as PictureModel,
                listener)
        }
    }

    /**
     * ViewHolder for header items in the RecyclerView.
     */
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.header_title)

        // Binds text used to the header.
        fun bind(header: String) {
            title.text = header
            itemView.setOnClickListener {
            }
        }
    }

    /**
     * ViewHolder for photo items in the RecyclerView.
     */
    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.imageView_photo)

        // Binds photo used to the picture model, making use of Glide. Lets them also be
        // clickable.
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

    /**
     * Sets a listener to handle item click events.
     *
     * @param listener The listener that will handle the item clicks.
     */
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    /**
     * An interface which gets the position of where the click was at in the recycler view.
     * */
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    /**
     * Gets item count of the amount of items within the recycler view.
     * */
    override fun getItemCount() = items.size

    /**
     * A companion object, used to assign different values for a header and a photo.
     */
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PHOTO = 1
    }
}

