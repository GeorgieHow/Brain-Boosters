package com.example.brainboosters

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.brainboosters.model.PictureModel

class QuizActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        //For making it full screen
        hideSystemUI()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_question_layout)

        val selectedPictures: List<PictureModel> = intent.getParcelableArrayListExtra<PictureModel>("selectedPictures") ?: arrayListOf<PictureModel>()

        val numOfPicsSelected = selectedPictures.size
        val noOfPictures = findViewById<TextView>(R.id.no_of_pics_picked)
        val nameOfFirstPicture = findViewById<TextView>(R.id.pic_2_test)

        noOfPictures.text = numOfPicsSelected.toString()
        nameOfFirstPicture.text = selectedPictures.get(0).imageName

    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}