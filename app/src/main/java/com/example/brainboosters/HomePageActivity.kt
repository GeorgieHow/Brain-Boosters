package com.example.brainboosters

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ismaeldivita.chipnavigation.ChipNavigationBar

class HomePageActivity : AppCompatActivity() {

    val fragment = HomeFragmentActivity()
    // Add a flag to track when UploadFragmentPart2 is displayed
    private var isUploadFragmentPart2Displayed = false

    /*override fun onResume() {
        super.onResume()
        val sharedPref = this.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val lastFragment = sharedPref.getString("LastFragment", null)
        navigateToFragment(lastFragment)
    }*/

    private fun navigateToFragment(fragmentName: String?) {
        val menuBottom = findViewById<ChipNavigationBar>(R.id.bottom_nav_bar)
        when (fragmentName) {
            "HomeFragment" -> {
                menuBottom.setItemSelected(R.id.nav_home, false) // `false` to not animate the click
                openMainFragment()
            }
            // Add cases for other fragments as needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)
        openMainFragment()

        val menuBottom = findViewById<ChipNavigationBar>(R.id.bottom_nav_bar)
        menuBottom.setItemSelected(R.id.nav_home)

        menuBottom.setOnItemSelectedListener { itemId ->
            // Check if trying to navigate away from UploadFragmentPart2
            if (isUploadFragmentPart2Displayed) {
                Toast.makeText(this, "Please complete the current process before navigating away", Toast.LENGTH_LONG).show()

                menuBottom.setItemSelected(R.id.nav_gallery, true)
            } else {
                when (itemId) {
                    R.id.nav_home -> {
                        openMainFragment()
                    }
                    R.id.nav_gallery -> {
                        val favoriteFragment = GalleryFragmentActivity()
                        changeFragment(favoriteFragment)
                    }
                    R.id.nav_statistics -> {
                        val profileFragment = StatisticsFragment()
                        changeFragment(profileFragment)
                    }
                    R.id.nav_profile -> {
                        val profileFragment = ProfileFragmentActivity()
                        changeFragment(profileFragment)
                    }
                }
            }
        }
    }

    fun changeFragment(fragment: Fragment, bundle: Bundle? = null) {
        bundle?.let {
            fragment.arguments = it
        }

        isUploadFragmentPart2Displayed = fragment is UploadFragmentPart2Activity
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, fragment).commit()
    }

    private fun openMainFragment() {
        changeFragment(fragment)
    }
}