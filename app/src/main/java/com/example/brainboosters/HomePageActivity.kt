package com.example.brainboosters

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ismaeldivita.chipnavigation.ChipNavigationBar

class HomePageActivity : AppCompatActivity() {

    val fragment = HomeFragmentActivity()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)
        openMainFragment()

        var menu_bottom = findViewById<ChipNavigationBar>(R.id.bottom_nav_bar)
        menu_bottom.setItemSelected(R.id.nav_home)

        menu_bottom.setOnItemSelectedListener {
            when (it) {

                R.id.nav_home -> {
                    openMainFragment()
                }
                R.id.nav_gallery -> {
                    val favoriteFragment = GalleryFragmentActivity()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, favoriteFragment).commit()

                }
                R.id.nav_statistics -> {
                    val profileFragment = StatisticsFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, profileFragment).commit()
                }
                R.id.nav_profile -> {
                    val profileFragment = ProfileFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, profileFragment).commit()
                }
            }
        }


    }
    fun changeFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, fragment)
            .commit()
    }

    private fun openMainFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainerView, fragment)
        transaction.commit()
    }
}