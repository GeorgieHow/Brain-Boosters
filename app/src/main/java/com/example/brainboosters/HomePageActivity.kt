package com.example.brainboosters

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.ismaeldivita.chipnavigation.ChipNavigationBar

/**
 * The main activity for the home page that manages navigation between different
 * fragments using a bottom navigation bar.
 */
class HomePageActivity : AppCompatActivity() {

    // Initialize the default home fragment
    val fragment = HomeFragmentActivity()

    // A flag to track if the second part of the upload process is currently displayed
    private var isUploadFragmentPart2Displayed = false

    fun setUploadFragmentPart2Displayed(displayed: Boolean) {
        isUploadFragmentPart2Displayed = displayed
    }

    fun isUploadFragmentPart2DisplayedGetter(): Boolean {
        return isUploadFragmentPart2Displayed
    }

    /**
     * Navigate to a specified fragment based on a given name.
     *
     * @param fragmentName The name identifier of the fragment to open.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)
        openMainFragment()

        val rootView = findViewById<View>(android.R.id.content)

        // Gets the bottom menu bar and sets it up for when its clicked.
        val menuBottom = findViewById<ChipNavigationBar>(R.id.bottom_nav_bar)
        menuBottom.setItemSelected(R.id.nav_home)
        menuBottom.setOnItemSelectedListener { itemId ->
            // Check if trying to navigate away from UploadFragmentPart2, stops user from doing it.
            if (isUploadFragmentPart2DisplayedGetter() == true) {
                Snackbar.make(rootView, "Please complete the current process before " +
                        "navigating away",
                    Snackbar.LENGTH_LONG).show()
                menuBottom.setItemSelected(R.id.nav_gallery, true)
            }
            // Handle navigation based on selected item.
            else {
                when (itemId) {
                    // Goes to Home fragment.
                    R.id.nav_home -> {
                        openMainFragment()
                    }
                    // Goes to Gallery fragment.
                    R.id.nav_gallery -> {
                        val galleryFragment = GalleryFragmentActivity()
                        changeFragment(galleryFragment)
                    }
                    // Goes to Statistics fragment.
                    R.id.nav_statistics -> {
                        val statisticsFragment = StatisticsFragmentActivity()
                        changeFragment(statisticsFragment)
                    }
                    // Goes to Profile fragment.
                    R.id.nav_profile -> {
                        val profileFragment = ProfileFragmentActivity()
                        changeFragment(profileFragment)
                    }
                }
            }
        }
    }

    /**
     * Change the current fragment displayed within the activity.
     *
     * @param fragment The new fragment to display.
     * @param bundle Optional data to pass to the fragment.
     */
    fun changeFragment(fragment: Fragment, bundle: Bundle? = null) {
        bundle?.let {
            fragment.arguments = it
        }

        // Track if the UploadFragmentPart2 is being displayed
        isUploadFragmentPart2Displayed = fragment is UploadFragmentPart2Activity
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, fragment).commit()
    }

    /**
     * Opens the HomeFragment.
     */
    private fun openMainFragment() {
        changeFragment(fragment)
    }
}