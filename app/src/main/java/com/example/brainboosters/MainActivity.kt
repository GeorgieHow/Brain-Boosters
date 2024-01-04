package com.example.brainboosters

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainboosters.ui.theme.BrainBoostersTheme
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {

    //Establishes firebase authentication
    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Log-in page
        setContentView(R.layout.activity_main)

        val enterButton = findViewById<Button>(R.id.enterButton)

        //Checks if user has an account and entered details are correct
        enterButton.setOnClickListener{
                view ->

            //Gets the ids for the fields and buttons
            val emailEntered = findViewById<EditText>(R.id.emailText)
            val passwordEntered = findViewById<EditText>(R.id.passwordText)

            val isEmailEmpty = emailEntered.text.toString()
            val isPasswordEmpty = passwordEntered.text.toString()

            if(isEmailEmpty.isNotEmpty() && isPasswordEmpty.isNotEmpty()) {

                mAuth.signInWithEmailAndPassword(
                    emailEntered.text.toString(),
                    passwordEntered.text.toString()
                )
                    .addOnCompleteListener(
                        this
                    ) { task ->
                        if (task.isSuccessful) {
                            //remove done and make them go to home page LOL.
                            Toast.makeText(this, "Account does exist",
                                Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, HomePageActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Account does not exist",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            else{
                Toast.makeText(this, "Don't leave fields empty", Toast.LENGTH_SHORT)
                    .show()
            }

        }


    }
}



